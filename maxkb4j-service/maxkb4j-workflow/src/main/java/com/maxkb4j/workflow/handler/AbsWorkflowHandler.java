package com.maxkb4j.workflow.handler;

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
import java.util.concurrent.atomic.AtomicReference;

import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;

/**
 * Abstract base class for workflow handlers.
 *
 * <p>Schedules READY/INTERRUPT nodes either on the workflowTaskExecutor (synchronous
 * handlers) or directly on the future returned by the handler (asynchronous streaming
 * handlers). Success completion is unified in {@link #completeNode} for both paths.</p>
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
        List<AtomicReference<Thread>> workerThreads = new ArrayList<>();
        for (AbsNode node : nodeList) {
            if (NodeStatus.READY.getStatus() == node.getStatus() || NodeStatus.INTERRUPT.getStatus() == node.getStatus()) {
                INodeHandler handler = nodeCenter.getHandler(node.getType());
                if (handler.isAsync()) {
                    // Async node: runs on its own future without occupying a workflowTaskExecutor thread
                    futureList.add(runAsyncChainNode(workflow, node));
                    workerThreads.add(null);
                } else {
                    // Sync node: runs on the workflowTaskExecutor
                    AtomicReference<Thread> workerThread = new AtomicReference<>();
                    futureList.add(CompletableFuture.supplyAsync(
                            () -> {
                                workerThread.set(Thread.currentThread());
                                try {
                                    return runChainNode(workflow, node);
                                } finally {
                                    workerThread.set(null);
                                }
                            },
                            workflowTaskExecutor));
                    workerThreads.add(workerThread);
                }
                scheduledNodes.add(node);
            } else if (NodeStatus.SKIP.getStatus() == node.getStatus()) {
                List<AbsNode> nextNodeList = workflow.execution().nextNodes(node, new NodeResult(Map.of()));
                nextNodeList.forEach(nextNode -> {
                    if (workflow.execution().isSkipNode(nextNode)) {
                        nextNode.setStatus(NodeStatus.SKIP.getStatus());
                    }
                });
                futureList.add(CompletableFuture.completedFuture(nextNodeList));
                workerThreads.add(null);
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
                // CompletableFuture.cancel 不会中断底层任务，这里显式中断执行线程，
                // 避免超时节点继续占用线程池资源并在结束后覆写节点状态
                AtomicReference<Thread> workerThread = workerThreads.get(i);
                if (workerThread != null) {
                    Thread worker = workerThread.get();
                    if (worker != null) {
                        worker.interrupt();
                    }
                }
                exceptionResolverChain.resolve(workflow, node, new RuntimeException("Node execution timeout after " + timeoutMinutes + " minutes"));
                node.setStatus(NodeStatus.ERROR.getStatus());
            } catch (Exception e) {
                exceptionResolverChain.resolve(workflow, node, e);
                node.setStatus(NodeStatus.ERROR.getStatus());
            }
        }
    }

    /**
     * Executes one synchronous node and returns the next nodes to schedule.
     */
    protected List<AbsNode> runChainNode(IWorkflow workflow, AbsNode node) {
        if (workflow.execution().dependenciesNotExecuted(node)) {
            return List.of();
        }
        NodeResult result = runNode(workflow, node);
        if (result != null) {
            NodeResultWriter.writeDetail(result, node);
            NodeResultWriter.writeContext(result, node, workflow);
        }
        if (NodeStatus.ERROR.getStatus() == node.getStatus()) {
            return List.of();
        }
        return completeNode(workflow, node, result);
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
        CompletableFuture<NodeResult> resultFuture;
        long startTime = System.currentTimeMillis();
        try {
            resultFuture = nodeHandler.execute(workflow, node);
        } catch (Exception ex) {
            // Synchronous exception thrown by execute() itself (e.g. pre-processing failure)
            handleNodeError(workflow, node, ex);
            return CompletableFuture.completedFuture(List.of());
        }
        return resultFuture.handle((result, ex) -> {
            if (ex != null) {
                Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                Exception realEx = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
                handleNodeError(workflow, node, realEx);
                return List.of();
            }
            recordExecutionTime(node, startTime);
            if (result != null) {
                NodeResultWriter.writeDetail(result, node);
                NodeResultWriter.writeContext(result, node, workflow);
            }
            return completeNode(workflow, node, result);
        });
    }

    /**
     * Completes a successfully executed node: applies the success status (or INTERRUPT when
     * the node asks to pause), fires the success hook and resolves the next nodes.
     */
    private List<AbsNode> completeNode(IWorkflow workflow, AbsNode node, NodeResult result) {
        node.setStatus(NodeStatus.SUCCESS.getStatus());
        if (result != null && NodeResultWriter.isInterruptExec(result, node)) {
            node.setStatus(NodeStatus.INTERRUPT.getStatus());
        }
        onNodeSuccess(workflow, node, result);
        return workflow.execution().nextNodes(node, result);
    }

    /**
     * Runs the node handler and converts failures into an error {@link NodeResult},
     * setting the outcome status (SUCCESS/ERROR) on the node directly.
     * Error handling is unified in the {@link ExceptionResolverChain}.
     */
    protected NodeResult runNode(IWorkflow workflow, AbsNode node) {
        try {
            onNodeStart(workflow, node);
            workflow.execution().recordExecution(node);
            INodeHandler nodeHandler = nodeCenter.getHandler(node.getType());
            long startTime = System.currentTimeMillis();
            NodeResult result = nodeHandler.execute(workflow, node).join();
            node.setStatus(NodeStatus.SUCCESS.getStatus());
            recordExecutionTime(node, startTime);
            return result;
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            Exception realEx = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
            return handleNodeError(workflow, node, realEx);
        } catch (Exception ex) {
            return handleNodeError(workflow, node, ex);
        }
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
    protected NodeResult handleNodeError(IWorkflow workflow, AbsNode node, Exception ex) {
        exceptionResolverChain.resolve(workflow, node, ex);
        node.setStatus(NodeStatus.ERROR.getStatus());
        return new NodeResult(Map.of());
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
        log.info("node ({}): {}, runTime: {} s", node.getStatus(), nodeName, runTime);
    }

}
