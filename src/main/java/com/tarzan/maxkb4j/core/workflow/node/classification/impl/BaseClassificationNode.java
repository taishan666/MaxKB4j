package com.tarzan.maxkb4j.core.workflow.node.classification.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.AI_CHAT;

public class BaseClassificationNode extends INode {

    private final ModelService modelService;

    public BaseClassificationNode() {
        this.type=AI_CHAT.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public NodeResult execute() {
        return null;
    }

    @Override
    public JSONObject getDetail() {
        return null;
    }
}
