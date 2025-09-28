package com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.imagegenerate.input.ImageGenerateParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_GENERATE;

public class ImageGenerateNode extends INode {

    private final ModelService modelService;

    public ImageGenerateNode(JSONObject properties) {
        super(properties);
        this.type=IMAGE_GENERATE.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public NodeResult execute() {
        ImageGenerateParams nodeParams=super.getNodeData().toJavaObject(ImageGenerateParams.class);
        String prompt=super.generatePrompt(nodeParams.getPrompt());
        String negativePrompt=nodeParams.getNegativePrompt();
        JSONObject modelParamsSetting=nodeParams.getModelParamsSetting();
        if (modelParamsSetting!=null){
            modelParamsSetting.put("negative_prompt",negativePrompt);
        }
        StringBuilder answerSb=new StringBuilder();
        List<String> imageUrls = new ArrayList<>();
        ImageModel imageModel = modelService.getModelById(nodeParams.getModelId(), modelParamsSetting);
        int n = modelParamsSetting == null ? 1 : modelParamsSetting.getIntValue("n");
        n=n==0 ? 1 : n;
        Response<List<Image>> res = imageModel.generate(prompt,n);
        List<Image> images = res.content();
        for (Image image : images) {
            String imageMd ="!["+prompt+"](" + image.url() + ")";
            answerSb.append(" ").append(imageMd);
            //todo url 是临时的url，需要保存到数据库中，并返回给前端
            imageUrls.add(image.url().toString());
        }
        return new NodeResult(Map.of("question",prompt,"answer",answerSb.toString(),"image",imageUrls),Map.of());
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
        context.put("image", detail.get("image"));
    }

    //todo 获取节点详情
    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("question",context.get("question"));
        detail.put("answer",context.get("answer"));
        detail.put("image",context.get("image"));
        return detail;
    }

}
