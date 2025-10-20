package com.tarzan.maxkb4j.core.workflow.node.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.APPLICATION;

public class ApplicationNode extends INode {

    public ApplicationNode(JSONObject properties) {
        super(properties);
        this.setType(APPLICATION.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("result", detail.get("result"));
    }




}