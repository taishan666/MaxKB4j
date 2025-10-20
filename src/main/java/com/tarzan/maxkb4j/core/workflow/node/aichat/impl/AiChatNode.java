package com.tarzan.maxkb4j.core.workflow.node.aichat.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.AI_CHAT;

@Slf4j
public class AiChatNode extends INode {


    public AiChatNode(JSONObject properties) {
        super(properties);
        super.setType(AI_CHAT.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
    }

}
