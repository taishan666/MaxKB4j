package com.maxkb4j.workflow.handler;

import com.maxkb4j.workflow.builder.NodeHandlerBuilder;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.NodeResultFuture;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;
import service.IWorkflowHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for workflow handlers.
 * Provides common runNodeFuture logic with template method pattern.
 */
@Slf4j
public abstract class AbsWorkflowHandler implements IWorkflowHandler {

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
                futureList.add(CompletableFuture.supplyAsync(() -> runChainNode(workflow, node)));
            }
            List<List<AbsNode>> nextNodeLists = futureList.stream()
                    .map(CompletableFuture::join)
                    .toList();
            for (List<AbsNode> nextNodeList : nextNodeLists) {
                runChainNodes(workflow, nextNodeList);
            }
        }
    }


    protected List<AbsNode> runChainNode(Workflow workflow, AbsNode node) {
        if (NodeStatus.READY.getStatus() == node.getStatus() || NodeStatus.INTERRUPT.getStatus() == node.getStatus()) {
            if (workflow.dependentNodeBeenExecuted(node)) {
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
                return workflow.getNextNodeList(node, nodeResult);
            }
        } else if (NodeStatus.SKIP.getStatus() == node.getStatus()) {
            // 获取下一个节点列表
            List<AbsNode> nextNodeList = workflow.getNextNodeList(node, new NodeResult(Map.of()));
            nextNodeList.forEach(nextNode -> {
                if (!workflow.isReadyJoinNode(nextNode)) {
                    nextNode.setStatus(NodeStatus.SKIP.getStatus());
                }
            });
            return nextNodeList;
        }
        return List.of();
    }

    protected NodeResultFuture runNodeFuture(Workflow workflow, AbsNode node) {
        try {
            long startTime = System.currentTimeMillis();

            // Hook for pre-execution logic (e.g., status updates)
            onNodeStart(workflow, node);

            INodeHandler nodeHandler = NodeHandlerBuilder.getHandler(node.getType());
            NodeResult result = nodeHandler.execute(workflow, node);

            float runTime = (System.currentTimeMillis() - startTime) / 1000F;
            node.getDetail().put("runTime", runTime);
            log.info("node:{}, runTime:{} s", node.getProperties().getString("nodeName"), runTime);

            // Hook for post-execution success logic
            onNodeSuccess(workflow, node, result);

            return new NodeResultFuture(result, null, NodeStatus.SUCCESS.getStatus());
        } catch (Exception ex) {
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
     * Subclasses can override to customize error handling.
     *
     * @param workflow the workflow context
     * @param node     the node that failed
     * @param ex       the exception that occurred
     * @return NodeResultFuture containing the error
     */
    protected NodeResultFuture handleNodeError(Workflow workflow, AbsNode node, Exception ex) {
        log.error("error:", ex);
        node.setErrMessage(ex.getMessage());
        log.error("NODE: {} Exception :{}", node.getType(), ex.getMessage());
        return new NodeResultFuture(null, ex, NodeStatus.ERROR.getStatus());
    }


}