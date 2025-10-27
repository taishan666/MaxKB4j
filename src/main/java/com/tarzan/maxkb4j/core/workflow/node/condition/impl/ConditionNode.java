package com.tarzan.maxkb4j.core.workflow.node.condition.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.CONDITION;

public class ConditionNode extends INode {

    public ConditionNode(JSONObject properties) {
        super(properties);
        this.setType(CONDITION.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("branchName", detail.get("branchName"));
    }


}
