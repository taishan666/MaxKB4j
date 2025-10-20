package com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_GENERATE;

public class ImageGenerateNode extends INode {


    public ImageGenerateNode(JSONObject properties) {
        super(properties);
        super.setType(IMAGE_GENERATE.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("answer", detail.get("answer"));
        context.put("image", detail.get("image"));
    }


}
