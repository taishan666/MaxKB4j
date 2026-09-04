package com.maxkb4j.workflow.handler.node;

import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.concurrent.CompletableFuture;

/**
 * Node handler contract: executes one workflow node and returns its result asynchronously.
 *
 * <p>Lifecycle concerns (timing, start message emission, failure propagation) are handled
 * by the {@link AbsNodeHandler} template, so implementations only provide the node logic.</p>
 */
public interface INodeHandler {

    /**
     * Executes the node.
     * Synchronous handlers return a completed future; streaming handlers return a future
     * that completes when the stream finishes.
     *
     * @param workflow the workflow context
     * @param node     the node instance
     * @return a future holding the node result
     * @throws Exception only for synchronous failures raised before the future is created
     */
    CompletableFuture<NodeResult> execute(IWorkflow workflow, AbsNode node) throws Exception;

    /**
     * Whether workflow execution should pause after this node (e.g. waiting for user input).
     */
    default boolean shouldInterrupt(AbsNode node) {
        return false;
    }

}
