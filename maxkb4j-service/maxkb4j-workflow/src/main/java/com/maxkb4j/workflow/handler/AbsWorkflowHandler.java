package com.maxkb4j.workflow.handler;

import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.NodeResultFuture;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import com.maxkb4j.workflow.service.IWorkflowHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Abstract base class for workflow handlers.
 * Provides common runNodeFuture logic with template method pattern.
 * Uses dedicated thread pool for parallel node execution.
 * Supports async node handlers that return CompletableFuture directly.
 */
@Slf4j
public abstract class AbsWorkflowHandler implements IWorkflowHandler {

    protected final NodeCenter nodeCenter;
    protected final Executor workflowTaskExecutor;
    protected final ExceptionResolverChain exceptionResolverChain;

    protected AbsWorkflowHandler(NodeCenter nodeCenter, Executor workflowTaskExecutor, ExceptionResolverChain exceptionResolverChain) {
        this.nodeCenter = nodeCenter;
        this.workflowTaskExecutor = workflowTaskExecutor;
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
        long timeoutMinutes = workflow.getNodeExecutionTimeoutMinutes();
        List<CompletableFuture<List<AbsNode>>> futureList = new ArrayList<>();
        for (AbsNode node : nodeList) {
            if (NodeStatus.READY.getStatus() == node.getStatus() || NodeStatus.INTERRUPT.getStatus() == node.getStatus()) {
                INodeHandler handler = nodeCenter.getHandler(node.getType());
                if (handler.isAsync()) {
                    // 异步节点：直接使用其返回的 CompletableFuture，不占用 workflowTaskExecutor 线程
                    futureList.add(runAsyncChainNode(workflow, node));
                } else {
                    // 同步节点：在 workflowTaskExecutor 上执行
                    futureList.add(CompletableFuture.supplyAsync(
                            () -> runChainNode(workflow, node),
                            workflowTaskExecutor));
                }
            } else if (NodeStatus.SKIP.getStatus() == node.getStatus()) {
                futureList.add(CompletableFuture.supplyAsync(
                        () -> {
                            // 获取下一个节点列表
                            List<AbsNode> nextNodeList = workflow.execution().nextNodes(node, new NodeResult(Map.of()));
                            nextNodeList.forEach(nextNode -> {
                                if (workflow.execution().isSkipNode(nextNode)) {
                                    nextNode.setStatus(NodeStatus.SKIP.getStatus());
                                }
                            });
                            return nextNodeList;
                        },
                        workflowTaskExecutor));

            }
        }
        for (int i = 0; i < futureList.size(); i++) {
            try {
                List<AbsNode> nextNodeList = futureList.get(i).get(timeoutMinutes, TimeUnit.MINUTES);
                runChainNodes(workflow, nextNodeList);
            } catch (TimeoutException e) {
                log.error("Node execution timeout after {} minutes", timeoutMinutes);
                futureList.get(i).cancel(true);
                AbsNode node = nodeList.get(i);
                // 统一使用责任链处理超时异常
                exceptionResolverChain.resolve(workflow, node, new RuntimeException("Node execution timeout after " + timeoutMinutes + " minutes"));
                node.setStatus(NodeStatus.ERROR.getStatus());
            } catch (Exception e) {
                AbsNode node = nodeList.get(i);
                // 统一使用责任链处理执行异常
                exceptionResolverChain.resolve(workflow, node, e);
                node.setStatus(NodeStatus.ERROR.getStatus());
            }
        }
    }


    protected List<AbsNode> runChainNode(Workflow workflow, AbsNode node) {
        if (workflow.execution().dependenciesExecuted(node)) {
            NodeResultFuture nodeResultFuture = runNodeFuture(workflow, node);
            node.setStatus(nodeResultFuture.getStatus());
            NodeResult nodeResult = nodeResultFuture.getResult();
            if (nodeResult != null) {
                nodeResult.writeDetail(node);
                nodeResult.writeContext(node, workflow);
                if (NodeStatus.ERROR.getStatus() == node.getStatus()) {
                    return List.of();
                }
                if (nodeResult.isInterruptExec(node)) {
                    node.setStatus(NodeStatus.INTERRUPT.getStatus());
                }
            }
            // 获取下一个节点列表
            return workflow.execution().nextNodes(node, nodeResult);
        }
        return List.of();
    }

    /**
     * 异步节点链式执行：直接使用节点返回的 CompletableFuture
     * 不阻塞 workflowTaskExecutor 线程，流式回调完成后自动推进
     */
    protected CompletableFuture<List<AbsNode>> runAsyncChainNode(Workflow workflow, AbsNode node) {
        onNodeStart(workflow, node);
        workflow.execution().recordExecution(node);
        INodeHandler nodeHandler = nodeCenter.getHandler(node.getType());
        CompletableFuture<NodeResult> resultFuture;
        try {
            resultFuture = nodeHandler.execute(workflow, node);
        } catch (Exception ex) {
            // execute() 方法本身抛出的同步异常（如预处理失败）
            NodeResultFuture errorResult = handleNodeError(workflow, node, ex);
            node.setStatus(errorResult.getStatus());
            return CompletableFuture.completedFuture(List.of());
        }
        return resultFuture.handle((result, ex) -> {
            if (ex != null) {
                Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                Exception realEx = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
                NodeResultFuture errorResult = handleNodeError(workflow, node, realEx);
                node.setStatus(errorResult.getStatus());
                return List.of();
            }
            node.setStatus(NodeStatus.SUCCESS.getStatus());
            if (result != null) {
                result.writeContext(node, workflow);
                result.writeDetail(node);
                if (result.isInterruptExec(node)) {
                    node.setStatus(NodeStatus.INTERRUPT.getStatus());
                }
            }
            onNodeSuccess(workflow, node, result);
            return workflow.execution().nextNodes(node, result);
        });
    }

    /**
     * 执行节点
     * 异常处理统一由 ExceptionResolverChain 责任链处理
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

            // 执行节点（同步节点返回 completedFuture，join 立即完成）
            CompletableFuture<NodeResult> future = nodeHandler.execute(workflow, node);
            NodeResult result = future.join();

            // 调用调度层成功钩子（用于后续调度逻辑）
            onNodeSuccess(workflow, node, result);

            return new NodeResultFuture(result, null, NodeStatus.SUCCESS.getStatus());

        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            Exception realEx = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
            return handleNodeError(workflow, node, realEx);
        } catch (Exception ex) {
            System.out.println("execute() method throws CompletionException212");
            // 统一异常处理：日志记录、详情记录、Sink发送
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
        NodeResult errorResult = new NodeResult(Map.of());
        return new NodeResultFuture(errorResult, ex, NodeStatus.ERROR.getStatus());
    }


}