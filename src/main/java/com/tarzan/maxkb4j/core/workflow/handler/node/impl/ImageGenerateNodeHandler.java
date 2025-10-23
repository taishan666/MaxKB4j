package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl.ImageGenerateNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Component
public class ImageGenerateNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        ImageGenerateNode.NodeParams nodeParams=node.getNodeData().toJavaObject(ImageGenerateNode.NodeParams.class);
        String prompt=workflow.generatePrompt(nodeParams.getPrompt());
        String negativePrompt=nodeParams.getNegativePrompt();
        JSONObject modelParamsSetting=nodeParams.getModelParamsSetting();
        if (modelParamsSetting!=null){
            modelParamsSetting.put("negative_prompt",negativePrompt);
        }
        StringBuilder answerSb=new StringBuilder();
        List<String> imageUrls = new ArrayList<>();
        ImageModel imageModel = modelFactory.build(nodeParams.getModelId(), modelParamsSetting);
        int n = modelParamsSetting == null ? 1 : modelParamsSetting.getIntValue("n");
        n=n==0 ? 1 : n;
        Response<List<Image>> res = imageModel.generate(prompt,n);
        List<Image> images = res.content();
        for (Image image : images) {
            String imageMd ="!["+prompt+"](" + image.url() + ")";
            answerSb.append(" ").append(imageMd);
            imageUrls.add(image.url().toString());
        }
        node.getDetail().put("question",prompt);
        return new NodeResult(Map.of("answer",answerSb.toString(),"image",imageUrls),Map.of());
    }
}
