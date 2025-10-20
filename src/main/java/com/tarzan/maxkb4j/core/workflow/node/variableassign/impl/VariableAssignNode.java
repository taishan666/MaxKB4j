package com.tarzan.maxkb4j.core.workflow.node.variableassign.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.VARIABLE_ASSIGN;

public class VariableAssignNode extends INode {
    public VariableAssignNode(JSONObject properties) {
        super(properties);
        this.setType(VARIABLE_ASSIGN.getKey());
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("variable_list", detail.get("variable_list"));
        context.put("resultList", detail.get("resultList"));
    }


}
