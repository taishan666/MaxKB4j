package com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.imagegenerate.input.ImageGenerateParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseImageGenerateNode extends INode {

    private final ModelService modelService;

    public BaseImageGenerateNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public String getType() {
        return "image-generate-node";
    }

    @Override
    public NodeResult execute() {
        ImageGenerateParams nodeParams=super.nodeParams.toJavaObject(ImageGenerateParams.class);
        String prompt=super.workflowManage.generatePrompt(nodeParams.getPrompt());
        String negativePrompt=nodeParams.getNegativePrompt();
        JSONObject modelParamsSetting=nodeParams.getModelParamsSetting();
        modelParamsSetting.put("negative_prompt",negativePrompt);
        ImageModel ttiModel = modelService.getModelById(nodeParams.getModelId(), modelParamsSetting);
        int n=modelParamsSetting.getInteger("n");
        Response<List<Image>> res = ttiModel.generate(prompt,n);
        StringBuilder answerSb=new StringBuilder();
        List<Image> images = res.content();
        List<String> imageUrls = new ArrayList<>();
        for (Image image : images) {
            String imageMd ="!["+prompt+"](" + image.url() + ")";
            answerSb.append(" ").append(imageMd);
            imageUrls.add(image.url().getPath());
        }

        return new NodeResult(Map.of("answer",answerSb.toString(),"image",imageUrls),Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer",context.get("answer"));
        return detail;
    }

}
