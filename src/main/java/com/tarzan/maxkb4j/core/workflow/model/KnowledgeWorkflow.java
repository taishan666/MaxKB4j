package com.tarzan.maxkb4j.core.workflow.model;

import com.tarzan.maxkb4j.core.workflow.enums.NodeStatus;
import com.tarzan.maxkb4j.core.workflow.enums.WorkflowMode;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.chat.dto.KnowledgeParams;
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
            super.setContext(knowledgeBase);
        }
    }


    public List<AbsNode> getStartNodes() {
        String nodeId = knowledgeParams.getDataSource().getNodeId();
        List<AbsNode> startNodes = super.getNodes().stream().filter(e -> !isTargetNode(e.getId())).toList();
        for (AbsNode startNode : startNodes) {
            if (!startNode.getId().equals(nodeId)) {
                startNode.setStatus(NodeStatus.SKIP.getCode());
            }
        }
        return startNodes;
    }

    public boolean isTargetNode(String nodeId) {
        return super.getEdges().stream().anyMatch(e -> e.getTargetNodeId().equals(nodeId));

    }


}
