package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class KnowledgeWorkflow extends Workflow {

    private KnowledgeParams knowledgeParams;

    public KnowledgeWorkflow(List<AbsNode> nodes, List<LfEdge> edges, KnowledgeParams knowledgeParams) {
        super(WorkflowMode.KNOWLEDGE, nodes, edges);
        this.knowledgeParams = knowledgeParams;
        Map<String, Object> knowledgeBase = knowledgeParams.getKnowledgeBase();
        if (knowledgeBase != null) {
            this.getContext().putAll(knowledgeBase);
        }
    }

    public List<AbsNode> getStartNodes() {
        String nodeId = knowledgeParams.getDataSource().getNodeId();
        List<AbsNode> startNodes = this.getConfiguration().getNodes().stream()
                .filter(e -> !isTargetNode(e.getId()))
                .toList();
        for (AbsNode startNode : startNodes) {
            if (!startNode.getId().equals(nodeId)) {
                startNode.setStatus(NodeStatus.SKIP.getStatus());
            }
        }
        return startNodes;
    }

    public boolean isTargetNode(String nodeId) {
        return this.getConfiguration().getEdges().stream()
                .anyMatch(e -> e.getTargetNodeId().equals(nodeId));
    }
}