package com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.imagegenerate.input.ImageGenerateParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_GENERATE;

public class ImageGenerateNode extends INode {


    public ImageGenerateNode(JSONObject properties) {
        super(properties);
        super.setType(IMAGE_GENERATE.getKey());
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
        context.put("image", detail.get("image"));
    }


}
