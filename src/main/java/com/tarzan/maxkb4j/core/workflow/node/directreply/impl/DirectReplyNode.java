package com.tarzan.maxkb4j.core.workflow.node.directreply.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.REPLY;

public class DirectReplyNode extends INode {

    public DirectReplyNode(JSONObject properties) {
        super(properties);
        super.setType(REPLY.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }



}
