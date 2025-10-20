package com.tarzan.maxkb4j.core.workflow.node.intentclassify.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.INTENT_CLASSIFY;

@Slf4j
public class IntentClassifyNode extends INode {


    public IntentClassifyNode(JSONObject properties) {
        super(properties);
        super.setType(INTENT_CLASSIFY.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
    }

}
