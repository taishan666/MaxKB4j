package com.maxkb4j.workflow.handler;

import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.NodeResultFuture;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IWorkflowHandler {
    void execute(Workflow workflow);
    NodeResultFuture runNodeFuture(Workflow workflow, AbsNode node);
    default void runChainNodes(Workflow workflow, List<AbsNode> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) {
            return;
        }
        if(nodeList.size()==1){
            List<AbsNode>  nextNodeList = runChainNode(workflow, nodeList.get(0));
            runChainNodes(workflow, nextNodeList);
        }else {
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

    default List<AbsNode> runChainNode(Workflow workflow, AbsNode node) {
        if (NodeStatus.READY.getStatus()==node.getStatus()||NodeStatus.INTERRUPT.getStatus()==node.getStatus()) {
            if (workflow.dependentNodeBeenExecuted(node)){
                NodeResultFuture nodeResultFuture = runNodeFuture(workflow, node);
                node.setStatus(nodeResultFuture.getStatus());
                NodeResult nodeResult = nodeResultFuture.getResult();
                if (nodeResult != null) {
                    nodeResult.writeContext(node, workflow);
                    nodeResult.writeDetail(node);
                    if(nodeResult.isInterruptExec(node)){
                        node.setStatus(NodeStatus.INTERRUPT.getStatus());
                    }
                }
                // 获取下一个节点列表
                return workflow.getNextNodeList(node, nodeResult);
            }
        }else if (NodeStatus.SKIP.getStatus()==node.getStatus()) {
            // 获取下一个节点列表
            List<AbsNode> nextNodeList = workflow.getNextNodeList(node, new NodeResult(Map.of()));
            nextNodeList.forEach(nextNode -> {
                if (!workflow.isReadyJoinNode(nextNode)){
                    nextNode.setStatus(NodeStatus.SKIP.getStatus());
                }
            });
            return nextNodeList;
        }
        return List.of();
    }
}



