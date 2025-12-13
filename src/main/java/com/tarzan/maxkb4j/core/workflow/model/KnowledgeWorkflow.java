package com.tarzan.maxkb4j.core.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class KnowledgeWorkflow extends Workflow {

    public static void main(String[] args) {
        System.out.println("hello world");
    }

    private JSONObject dataSource;

    public KnowledgeWorkflow(List<INode> nodes, List<LfEdge> edges,JSONObject dataSource, Map<String, Object> knowledgeBase) {
        super(nodes, edges);
        this.dataSource = dataSource;
        super.setContext(knowledgeBase);
    }

    @Override
    public INode getStartNode() {
        String nodeId = this.dataSource.getString("node_id");
        return super.getNodes().stream().filter(e -> e.getId().equals(nodeId)).findFirst().orElse(null);
    }


}
