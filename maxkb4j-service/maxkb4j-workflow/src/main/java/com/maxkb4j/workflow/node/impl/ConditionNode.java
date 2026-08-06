package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.CONDITION;


@NodeCreatorType(NodeType.CONDITION)
public class ConditionNode extends AbsNode {

    public ConditionNode(String id, JSONObject properties) {
        super(id, properties);
        this.setType(CONDITION.getKey());
    }


    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("branchName", detail.get("branchName"));
    }

}
