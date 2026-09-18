package com.maxkb4j.workflow.handler;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.engine.NodeResultWriter;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.INode;
import com.maxkb4j.workflow.registry.NodeCenter;
import com.maxkb4j.workflow.service.IWorkflowHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;

/**
 * Abstract base class for workflow handlers.
 *
 * <p>Schedules READY nodes on the calling thread and chains completion on the future
 * returned by each node handler, so streaming handlers do not occupy a dedicated
 * executor thread. Success completion is unified in {@link #completeNode} for both
 * paths.</p>
 */
@Slf4j
public abstract class AbsWorkflowHandler implements IWorkflowHandler {

    protected final NodeCenter nodeCenter;
    protected final ExceptionResolverChain exceptionResolverChain;

    protected AbsWorkflowHandler(NodeCenter nodeCenter, ExceptionResolverChain exceptionResolverChain) {
        this.nodeCenter = nodeCenter;
        this.exceptionResolverChain = exceptionResolverChain;
    }

    /**
     * Unwraps the real exception cause from {@link CompletionException}/{@link ExecutionException}
     * chains, converting non-{@link Exception} throwables into a {@link RuntimeException}.
     */
    private static Exception unwrapException(Throwable cause) {
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
    }

    @Override
    public void execute(IWorkflow workflow) {
        INode currentNode = workflow.execution().currentNode();
        List<INode> startNodes = currentNode == null ? workflow.startNodes() : List.of(currentNode);
        log.info("{} workflow started", workflow.getWorkflowMode());
        onProcessStart(workflow);
        try {
            runChainNodes(workflow, startNodes);
            onProcessCompleted(workflow);
            log.info("{} workflow completed", workflow.getWorkflowMode());
        } catch (Exception e) {
            log.error("{} workflow failed", workflow.getWorkflowMode(), e);
            throw e;
        }
    }

    protected void runChainNodes(IWorkflow workflow, List<INode> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) {
            return;
        }
        long timeoutMinutes = workflow.getNodeExecutionTimeoutMinutes();
        for (ScheduledNode scheduled : scheduleNodes(workflow, nodeList)) {
            AbsNode node = scheduled.node();
            try {
                List<INode> nextNodeList = scheduled.future().get(timeoutMinutes, TimeUnit.MINUTES);
                runChainNodes(workflow, nextNodeList);
            } catch (TimeoutException e) {
                scheduled.future().cancel(true);
                handleNodeError(workflow, node, new TimeoutException(
                        "node [" + node.getType() + "] execution timeout after " + timeoutMinutes + " minutes"));
            } catch (ExecutionException e) {
                handleNodeError(workflow, node, unwrapException(e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for node [{}] execution", node.getType());
                break;
            }
        }
    }

    /**
     * Schedules the runnable nodes of one batch, pairing each with its result future.
     * A node that fails to schedule is resolved through {@link #handleNodeError} instead
     * of aborting the whole chain.
     */
    private List<ScheduledNode> scheduleNodes(IWorkflow workflow, List<INode> nodeList) {
        List<ScheduledNode> scheduledNodes = new ArrayList<>(nodeList.size());
        for (INode entry : nodeList) {
            // 契约节点在引擎内统一为 AbsNode（INode 的引擎实现）
            AbsNode node = (AbsNode) entry;
            try {
                if (NodeStatus.READY.getStatus() == node.getStatus()) {
                    scheduledNodes.add(new ScheduledNode(node, runAsyncChainNode(workflow, node)));
                } else if (NodeStatus.SKIP.getStatus() == node.getStatus()) {
                    scheduledNodes.add(new ScheduledNode(node, skipNode(workflow, node)));
                }
            } catch (Exception e) {
                handleNodeError(workflow, node, e);
            }
        }
        return scheduledNodes;
    }

    /**
     * Propagates the SKIP status to the successors of an already skipped node.
     */
    private CompletableFuture<List<INode>> skipNode(IWorkflow workflow, AbsNode node) {
        List<INode> nextNodeList = workflow.execution().nextNodes(node, new NodeResult(Map.of()));
        nextNodeList.forEach(nextNode -> nextNode.setStatus(NodeStatus.SKIP.getStatus()));
        return CompletableFuture.completedFuture(nextNodeList);
    }

    /**
     * Executes one asynchronous node on the future returned by its handler, so streaming
     * does not occupy a dedicated executor thread.
     */
    protected CompletableFuture<List<INode>> runAsyncChainNode(IWorkflow workflow, AbsNode node) {
        if (workflow.execution().dependenciesNotExecuted(node)) {
            return CompletableFuture.completedFuture(List.of());
        }
        onNodeStart(workflow, node);
        workflow.execution().recordExecution(node);
        INodeHandler nodeHandler = nodeCenter.getHandler(node.getType());
        long startTime = System.currentTimeMillis();
        return startNodeExecution(nodeHandler, workflow, node)
                .handle((result, ex) -> completeAsyncNode(workflow, node, startTime, result, ex));
    }

    /**
     * Invokes the node handler, funneling a synchronously thrown exception into an
     * exceptionally completed future, so that both failure modes converge on the
     * single {@link #completeAsyncNode} completion path.
     */
    private static CompletableFuture<NodeResult> startNodeExecution(INodeHandler nodeHandler, IWorkflow workflow, AbsNode node) {
        try {
            return nodeHandler.execute(workflow, node);
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    /**
     * Completes an asynchronously executed node: converts failures into an empty node list
     * and delegates successful results to {@link #completeNode}.
     */
    private List<INode> completeAsyncNode(IWorkflow workflow, AbsNode node, long startTime, NodeResult result, Throwable ex) {
        recordExecutionTime(node, startTime);
        if (ex != null) {
            return completeFailedNode(workflow, node, ex);
        }
        return completeNode(workflow, node, result);
    }

    /**
     * Completes a successfully executed node: applies the SUCCESS status, fires the success
     * hook and resolves the next nodes.
     */
    private List<INode> completeNode(IWorkflow workflow, AbsNode node, NodeResult result) {
        node.setStatus(NodeStatus.SUCCESS.getStatus());
        if (result != null) {
            writeResult(result, node, workflow);
        }
        onNodeSuccess(workflow, node, result);
        return workflow.execution().nextNodes(node, result);
    }

    /**
     * Completes a failed node: routes execution to the configured exception branch when
     * the node enables it, otherwise resolves the error through {@link #handleNodeError}
     * and stops this branch with an empty successor list.
     */
    private List<INode> completeFailedNode(IWorkflow workflow, AbsNode node, Throwable ex) {
        handleNodeError(workflow, node, unwrapException(ex));
        NodeResult result = new NodeResult(Map.of(NodeField.EXCEPTION, ex.getMessage()));
        Boolean enableException = node.getProperties().getBoolean(NodeField.ENABLE_EXCEPTION);
        if (Boolean.TRUE.equals(enableException)) {
            result = new NodeResult(Map.of(
                    NodeField.BRANCH_ID, NodeField.EXCEPTION,
                    NodeField.EXCEPTION, ex.getMessage()));
            return workflow.execution().nextNodes(node, result);
        }
        writeResult(result, node, workflow);
        return List.of();
    }

    /**
     * Writes the node result into the node detail and the workflow context.
     */
    private void writeResult(NodeResult result, AbsNode node, IWorkflow workflow) {
        NodeResultWriter.writeDetail(result, node);
        NodeResultWriter.writeContext(result, node, workflow);
    }



    /**
     * Hook called before node execution; subclasses may override for scheduling logic.
     */
    protected void onNodeStart(IWorkflow workflow, AbsNode node) {
    }

    /**
     * Hook called after successful node execution; subclasses may override.
     */
    protected void onNodeSuccess(IWorkflow workflow, AbsNode node, NodeResult result) {
    }

    /**
     * Hook called when the workflow process starts; subclasses may override.
     */
    protected void onProcessStart(IWorkflow workflow) {
    }

    /**
     * Hook called when the workflow process completes; subclasses may override.
     */
    protected void onProcessCompleted(IWorkflow workflow) {
    }

    /**
     * Handles node execution errors through the {@link ExceptionResolverChain}
     * and marks the node as ERROR.
     */
    protected void handleNodeError(IWorkflow workflow, AbsNode node, Exception ex) {
        exceptionResolverChain.resolve(workflow, node, ex);
        node.setStatus(NodeStatus.ERROR.getStatus());
    }

    /**
     * Records the node execution time (seconds) into the node detail.
     */
    protected void recordExecutionTime(AbsNode node, long startTime) {
        float runTime = (System.currentTimeMillis() - startTime) / 1000F;
        node.getDetail().put(RuntimeDetailField.RUN_TIME, runTime);
        log.info("node: {}, runTime: {} s", resolveNodeName(node), runTime);
    }

    private static String resolveNodeName(AbsNode node) {
        JSONObject properties = node.getProperties();
        if (properties == null) {
            return node.getType();
        }
        String nodeName = properties.getString(RuntimeDetailField.NODE_NAME);
        return nodeName != null ? nodeName : node.getType();
    }

    /**
     * A node scheduled in one batch, paired with the future of its successor list.
     */
    private record ScheduledNode(AbsNode node, CompletableFuture<List<INode>> future) {
    }

}
