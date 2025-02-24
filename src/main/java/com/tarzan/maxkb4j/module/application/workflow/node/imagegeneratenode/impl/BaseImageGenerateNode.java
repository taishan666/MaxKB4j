package com.tarzan.maxkb4j.module.application.workflow.node.imagegeneratenode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.node.imagegeneratenode.IImageGenerateNode;
import com.tarzan.maxkb4j.module.application.workflow.node.imagegeneratenode.dto.ImageGenerateParams;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.Map;

public class BaseImageGenerateNode extends IImageGenerateNode {

    private final ModelService modelService;

    public BaseImageGenerateNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
    }
    @Override
    public NodeResult execute(ImageGenerateParams nodeParams) {
        String prompt=super.workflowManage.generatePrompt(nodeParams.getPrompt());
        String negativePrompt=nodeParams.getNegativePrompt();
        ImageModel ttiModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        Response<Image> res = ttiModel.generate(prompt+negativePrompt);
        Image image = res.content();
        System.out.println(image.url());
        String answer ="!["+prompt+"](" + image.url() + ")";
        return new NodeResult(Map.of("answer",answer,"image",image.url()),Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer",context.get("answer"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
