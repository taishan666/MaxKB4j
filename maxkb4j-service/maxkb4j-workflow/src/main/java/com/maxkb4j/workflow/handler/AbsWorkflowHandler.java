package com.maxkb4j.workflow.handler;

import com.maxkb4j.workflow.consts.WorkflowConstants;
import com.maxkb4j.workflow.engine.NodeResultWriter;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import com.maxkb4j.workflow.service.IWorkflowHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;

/**
 * Abstract base class for workflow handlers.
 *
 * <p>Schedules READY nodes either on the workflowTaskExecutor (synchronous handlers) or
 * directly on the future returned by the handler (asynchronous streaming handlers).
 * Success completion is unified in {@link #completeNode} for both paths.</p>
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
    public void execute(IWorkflow workflow) {
        AbsNode currentNode = workflow.execution().currentNode();
        List<AbsNode> startNodes = currentNode == null ? workflow.startNodes() : List.of(currentNode);
        log.info("{} workflow started", workflow.getWorkflowMode());
        onProcessStart(workflow);
        runChainNodes(workflow, startNodes);
        onProcessCompleted(workflow);
        log.info("{} workflow completed", workflow.getWorkflowMode());
    }

    protected void runChainNodes(IWorkflow workflow, List<AbsNode> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) {
            return;
        }
        long timeoutMinutes = workflow.getNodeExecutionTimeoutMinutes();
        List<CompletableFuture<List<AbsNode>>> futureList = new ArrayList<>();
        List<AbsNode> scheduledNodes = new ArrayList<>();
        for (AbsNode node : nodeList) {
            if (NodeStatus.READY.getStatus() == node.getStatus()) {
                futureList.add(runAsyncChainNode(workflow, node));
                scheduledNodes.add(node);
            } else if (NodeStatus.SKIP.getStatus() == node.getStatus()) {
                List<AbsNode> nextNodeList = workflow.execution().nextNodes(node, new NodeResult(Map.of()));
                nextNodeList.forEach(nextNode -> nextNode.setStatus(NodeStatus.SKIP.getStatus()));
                futureList.add(CompletableFuture.completedFuture(nextNodeList));
                scheduledNodes.add(node);
            }
        }
        for (int i = 0; i < futureList.size(); i++) {
            CompletableFuture<List<AbsNode>> future = futureList.get(i);
            AbsNode node = scheduledNodes.get(i);
            try {
                List<AbsNode> nextNodeList = future.get(timeoutMinutes, TimeUnit.MINUTES);
                runChainNodes(workflow, nextNodeList);
            } catch (TimeoutException e) {
                log.error("Node execution timeout after {} minutes", timeoutMinutes);
                future.cancel(true);
            } catch (ExecutionException e) {
                log.error("Node execution error: {}", e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for node {} execution", node.getType());
                break;
            }
        }
    }

    /**
     * Executes one asynchronous node on the future returned by its handler, so streaming
     * does not occupy a workflowTaskExecutor thread.
     */
    protected CompletableFuture<List<AbsNode>> runAsyncChainNode(IWorkflow workflow, AbsNode node) {
        if (workflow.execution().dependenciesNotExecuted(node)) {
            return CompletableFuture.completedFuture(List.of());
        }
        onNodeStart(workflow, node);
        workflow.execution().recordExecution(node);
        INodeHandler nodeHandler = nodeCenter.getHandler(node.getType());
        long startTime = System.currentTimeMillis();
        CompletableFuture<NodeResult> resultFuture;
        try {
            resultFuture = nodeHandler.execute(workflow, node);
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(completeAsyncNode(workflow, node, startTime, null, ex));
        }
        return resultFuture.handle((result, ex) -> completeAsyncNode(workflow, node, startTime, result, ex));
    }

    /**
     * Completes an asynchronously executed node: converts failures into an empty node list
     * and delegates successful results to {@link #completeNode}.
     */
    private List<AbsNode> completeAsyncNode(IWorkflow workflow, AbsNode node, long startTime, NodeResult result, Throwable ex) {
        if (ex != null) {
            handleNodeError(workflow, node, unwrapException(ex));
            Boolean enableException = node.getProperties().getBoolean("enableException");
            if (Boolean.TRUE.equals(enableException)){
                result= new NodeResult(Map.of(WorkflowConstants.NodeField.BRANCH_ID,"exception","exception",ex.getMessage()));
                return workflow.execution().nextNodes(node, result);
            }
            return List.of();
        }
        recordExecutionTime(node, startTime);
        if (result != null) {
            writeResult(result, node, workflow);
        }
        return completeNode(workflow, node, result);
    }

    /**
     * Writes the node result into the node detail and the workflow context.
     */
    private void writeResult(NodeResult result, AbsNode node, IWorkflow workflow) {
        NodeResultWriter.writeDetail(result, node);
        NodeResultWriter.writeContext(result, node, workflow);
    }

    /**
     * Completes a successfully executed node: applies the SUCCESS status, fires the success
     * hook and resolves the next nodes.
     */
    private List<AbsNode> completeNode(IWorkflow workflow, AbsNode node, NodeResult result) {
        node.setStatus(NodeStatus.SUCCESS.getStatus());
        onNodeSuccess(workflow, node, result);
        return workflow.execution().nextNodes(node, result);
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
     * Hook called after successful node execution; subclasses may override.
     */
    protected void onProcessStart(IWorkflow workflow) {
    }

    /**
     * Hook called after successful node execution; subclasses may override.
     */
    protected void onProcessCompleted(IWorkflow workflow) {
    }

    /**
     * Handles node execution errors through the {@link ExceptionResolverChain}.
     * Sets ERROR status on the node and returns an empty error NodeResult.
     */
    protected void handleNodeError(IWorkflow workflow, AbsNode node, Exception ex) {
        exceptionResolverChain.resolve(workflow, node, ex);
        node.setStatus(NodeStatus.ERROR.getStatus());
    }

    /**
     * Records the node execution time (seconds) into the node detail.
     */
    protected void recordExecutionTime(AbsNode node, long startTime) {
        long endTime = System.currentTimeMillis();
        float runTime = (endTime - startTime) / 1000F;
        node.getDetail().put(RuntimeDetailField.RUN_TIME, runTime);
        String nodeName = node.getProperties() != null
                ? node.getProperties().getString(RuntimeDetailField.NODE_NAME)
                : node.getType();
        log.info("node: {}, runTime: {} s", nodeName, runTime);
    }

    /**
     * Unwraps the real exception cause from a {@link CompletionException} chain, converting
     * non-{@link Exception} throwables into a {@link RuntimeException}.
     */
    private static Exception unwrapException(Throwable cause) {
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
    }

}
