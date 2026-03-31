package com.maxkb4j.workflow.handler;

import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.NodeResultFuture;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.model.WorkflowOutputManager;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import com.maxkb4j.workflow.service.IWorkflowHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Abstract base class for workflow handlers.
 * Provides common runNodeFuture logic with template method pattern.
 * Uses dedicated thread pool for parallel node execution.
 */
@Slf4j
public abstract class AbsWorkflowHandler implements IWorkflowHandler {

    protected final NodeCenter nodeCenter;
    protected final Executor workflowExecutor;
    protected final ExceptionResolverChain exceptionResolverChain;

    protected AbsWorkflowHandler(NodeCenter nodeCenter, Executor workflowExecutor, ExceptionResolverChain exceptionResolverChain) {
        this.nodeCenter = nodeCenter;
        this.workflowExecutor = workflowExecutor;
        this.exceptionResolverChain = exceptionResolverChain;
    }

    @Override
    public void execute(Workflow workflow) {
        AbsNode currentNode = workflow.execution().currentNode();
        if (currentNode == null) {
            currentNode = workflow.execution().startNode();
        }
        log.info("Workflow started");
        runChainNodes(workflow, List.of(currentNode));
        log.info("Workflow completed");
    }

    protected void runChainNodes(Workflow workflow, List<AbsNode> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) {
            return;
        }
        if (nodeList.size() == 1) {
            List<AbsNode> nextNodeList = runChainNode(workflow, nodeList.get(0));
            runChainNodes(workflow, nextNodeList);
        } else {
            List<CompletableFuture<List<AbsNode>>> futureList = new ArrayList<>();
            for (AbsNode node : nodeList) {
                futureList.add(CompletableFuture.supplyAsync(
                        () -> runChainNode(workflow, node),
                        workflowExecutor));
            }
            long timeoutSeconds = workflow.getNodeExecutionTimeoutSeconds();
            for (int i = 0; i < futureList.size(); i++) {
                try {
                    List<AbsNode> nextNodeList = futureList.get(i).get(timeoutSeconds, TimeUnit.SECONDS);
                    runChainNodes(workflow, nextNodeList);
                } catch (TimeoutException e) {
                    log.error("Node execution timeout after {} seconds", timeoutSeconds);
                    futureList.get(i).cancel(true);
                    AbsNode node = nodeList.get(i);
                    node.setErrMessage("Node execution timeout after " + timeoutSeconds + " seconds");
                    node.setStatus(NodeStatus.ERROR.getStatus());
                } catch (Exception e) {
                    log.error("Node execution failed: {}", e.getMessage(), e);
                    AbsNode node = nodeList.get(i);
                    node.setErrMessage(e.getMessage());
                    node.setStatus(NodeStatus.ERROR.getStatus());
                }
            }
        }
    }


    protected List<AbsNode> runChainNode(Workflow workflow, AbsNode node) {
        if (NodeStatus.READY.getStatus() == node.getStatus() || NodeStatus.INTERRUPT.getStatus() == node.getStatus()) {
            if (workflow.execution().dependenciesExecuted(node)) {
                NodeResultFuture nodeResultFuture = runNodeFuture(workflow, node);
                node.setStatus(nodeResultFuture.getStatus());
                NodeResult nodeResult = nodeResultFuture.getResult();
                if (nodeResult != null) {
                    nodeResult.writeContext(node, workflow);
                    nodeResult.writeDetail(node);
                    if (nodeResult.isInterruptExec(node)) {
                        node.setStatus(NodeStatus.INTERRUPT.getStatus());
                    }
                }
                // 获取下一个节点列表
                return workflow.execution().nextNodes(node, nodeResult);
            }
        } else if (NodeStatus.SKIP.getStatus() == node.getStatus()) {
            // 获取下一个节点列表
            List<AbsNode> nextNodeList = workflow.execution().nextNodes(node, new NodeResult(java.util.Map.of()));
            nextNodeList.forEach(nextNode -> {
                if (!workflow.execution().isReadyJoin(nextNode)) {
                    nextNode.setStatus(NodeStatus.SKIP.getStatus());
                }
            });
            return nextNodeList;
        }
        return List.of();
    }

    /**
     * 执行节点
     * 简化后的执行方法，所有执行逻辑集中在 AbstractNodeHandler.execute()
     *
     * Note: AbstractNodeHandler.execute() already handles onError internally,
     * so we don't need to call it again here to avoid duplicate error handling.
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @return 执行结果Future
     */
    protected NodeResultFuture runNodeFuture(Workflow workflow, AbsNode node) {
        try {
            // 获取处理器
            INodeHandler nodeHandler = nodeCenter.getHandler(node.getType());

            // 调用调度层钩子（用于状态更新等调度逻辑）
            onNodeStart(workflow, node);

            // 记录执行轨迹
            workflow.execution().recordExecution(node);

            // 执行节点 - 所有执行逻辑（包括时间记录、钩子调用、onError）都在 Handler 内部处理
            NodeResult result = nodeHandler.execute(workflow, node);

            // 调用调度层成功钩子（用于后续调度逻辑）
            onNodeSuccess(workflow, node, result);

            return new NodeResultFuture(result, null, NodeStatus.SUCCESS.getStatus());

        } catch (Exception ex) {
            // AbstractNodeHandler.execute() already called onError, just handle scheduling-level error
            return handleNodeError(workflow, node, ex);
        }
    }

    /**
     * Hook method called before node execution.
     * Subclasses can override to add pre-execution logic.
     *
     * @param workflow the workflow context
     * @param node     the node to be executed
     */
    protected void onNodeStart(Workflow workflow, AbsNode node) {
        // Default: do nothing
    }

    /**
     * Hook method called after successful node execution.
     * Subclasses can override to add post-execution logic.
     *
     * @param workflow the workflow context
     * @param node     the executed node
     * @param result   the execution result
     */
    protected void onNodeSuccess(Workflow workflow, AbsNode node, NodeResult result) {
        // Default: do nothing
    }

    /**
     * Handles errors during node execution.
     * Uses ExceptionResolverChain for unified error handling.
     *
     * @param workflow the workflow context
     * @param node     the node that failed
     * @param ex       the exception that occurred
     * @return NodeResultFuture containing the error
     */
    protected NodeResultFuture handleNodeError(Workflow workflow, AbsNode node, Exception ex) {
        // 使用统一的异常解析器链处理异常
        exceptionResolverChain.resolve(workflow, node, ex);
        return new NodeResultFuture(null, ex, NodeStatus.ERROR.getStatus());
    }


}