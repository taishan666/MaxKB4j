package com.tarzan.maxkb4j.core.workflow.model;

import com.tarzan.maxkb4j.core.workflow.enums.NodeStatus;
import com.tarzan.maxkb4j.core.workflow.enums.WorkflowMode;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.module.chat.dto.KnowledgeParams;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KnowledgeWorkflow extends Workflow {

    private KnowledgeParams knowledgeParams;


    public KnowledgeWorkflow(KnowledgeParams knowledgeParams,List<INode> nodes, List<LfEdge> edges) {
        super(WorkflowMode.KNOWLEDGE,nodes, edges);
        this.knowledgeParams = knowledgeParams;
    }


    public List<INode> getStartNodes() {
        String nodeId = knowledgeParams.getDataSource().getNodeId();
        List<INode> startNodes = super.getNodes().stream().filter(e -> !isTargetNode(e.getId())).toList();
        for (INode startNode : startNodes) {
            if (!startNode.getId().equals(nodeId)){
                startNode.setStatus(NodeStatus.SKIP.getCode());
            }
        }
        return startNodes;
    }

    public boolean isTargetNode(String nodeId) {
       return super.getEdges().stream().anyMatch(e -> e.getTargetNodeId().equals(nodeId));

    }


}
