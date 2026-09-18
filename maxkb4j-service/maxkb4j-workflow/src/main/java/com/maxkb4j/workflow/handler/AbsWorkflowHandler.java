package com.maxkb4j.workflow.handler;

import com.maxkb4j.workflow.engine.NodeChainRunner;
import com.maxkb4j.workflow.engine.NodeExecutionListener;
import com.maxkb4j.workflow.engine.NodeLifecycleExecutor;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.INode;
import com.maxkb4j.workflow.registry.NodeCenter;
import com.maxkb4j.workflow.service.IWorkflowHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Abstract base class for workflow handlers.
 *
 * <p>Template method only: entry selection, typed dispatch and lifecycle hooks.
 * Scheduling and per-node completion semantics live in the engine collaborators
 * {@link NodeChainRunner} and {@link NodeLifecycleExecutor}, which report node
 * events back through {@link NodeExecutionListener}.</p>
 *
 * <p>The type token binds the workflow type once: {@link #canHandle} and the
 * {@code onNode*} hooks are narrowed to {@code W}, so subclasses no longer guard
 * with {@code instanceof} nor keep {@code canHandle} consistent with their hooks
 * by hand. Subclasses extend {@code AbsWorkflowHandler<TheirWorkflow>} and
 * override the typed hooks.</p>
 *
 * @param <W> the workflow contract this handler accepts
 */
@Slf4j
public abstract class AbsWorkflowHandler<W extends IWorkflow>
        implements IWorkflowHandler, NodeExecutionListener<W> {

    private final Class<W> workflowType;
    private final NodeChainRunner<W> chainRunner;

    /**
     * Resolves node exceptions and marks the node ERROR; subclasses may extend
     * (e.g. emit an error frame) by overriding {@link #onNodeError}.
     */
    protected final ExceptionResolverChain exceptionResolverChain;

    protected AbsWorkflowHandler(Class<W> workflowType,
                                 NodeCenter nodeCenter,
                                 ExceptionResolverChain exceptionResolverChain) {
        this.workflowType = workflowType;
        this.exceptionResolverChain = exceptionResolverChain;
        this.chainRunner = new NodeChainRunner<>(new NodeLifecycleExecutor<>(nodeCenter, this));
    }

    @Override
    public final boolean canHandle(IWorkflow workflow) {
        return workflowType.isInstance(workflow);
    }

    @Override
    public final void execute(IWorkflow workflow) {
        W typedWorkflow = workflowType.cast(workflow);
        INode currentNode = typedWorkflow.execution().currentNode();
        List<INode> startNodes = currentNode == null ? typedWorkflow.startNodes() : List.of(currentNode);
        log.info("{} workflow started", typedWorkflow.getWorkflowMode());
        onProcessStart(typedWorkflow);
        try {
            chainRunner.run(typedWorkflow, startNodes);
            onProcessCompleted(typedWorkflow);
            log.info("{} workflow completed", typedWorkflow.getWorkflowMode());
        } catch (Exception e) {
            log.error("{} workflow failed", typedWorkflow.getWorkflowMode(), e);
            throw e;
        }
    }

    /**
     * Node started: fired after the dependency check passes and before the node
     * handler is invoked; subclasses may override for side effects.
     */
    @Override
    public void onNodeStart(W workflow, AbsNode node) {
    }

    /**
     * Node completed successfully; subclasses may override.
     */
    @Override
    public void onNodeSuccess(W workflow, AbsNode node, NodeResult result) {
    }

    /**
     * Node scheduling or execution failed: resolves the exception through the
     * {@link ExceptionResolverChain} and marks the node ERROR.
     */
    @Override
    public void onNodeError(W workflow, AbsNode node, Exception ex) {
        exceptionResolverChain.resolve(workflow, node, ex);
        node.setStatus(NodeStatus.ERROR.getStatus());
    }

    /**
     * Hook called when the workflow process starts; subclasses may override.
     */
    protected void onProcessStart(W workflow) {
    }

    /**
     * Hook called when the workflow process completes; subclasses may override.
     */
    protected void onProcessCompleted(W workflow) {
    }
}
