package com.tarzan.maxkb4j.core.workflow.node.imageunderstand.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_UNDERSTAND;

@Slf4j
public class ImageUnderstandNode extends INode {


    public ImageUnderstandNode(JSONObject properties) {
        super(properties);
        super.setType(IMAGE_UNDERSTAND.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }



}
