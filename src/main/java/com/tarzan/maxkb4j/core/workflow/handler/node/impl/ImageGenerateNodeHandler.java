package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ImageGenerateNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.IMAGE_UNDERSTAND)
@RequiredArgsConstructor
@Component
public class ImageGenerateNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        ImageGenerateNode.NodeParams nodeParams=node.getNodeData().toJavaObject(ImageGenerateNode.NodeParams.class);
        String prompt=workflow.generatePrompt(nodeParams.getPrompt());
        String negativePrompt=nodeParams.getNegativePrompt();
        JSONObject modelParamsSetting=nodeParams.getModelParamsSetting();
        if (modelParamsSetting!=null){
            modelParamsSetting.put("negative_prompt",negativePrompt);
        }
        ImageModel imageModel = modelFactory.buildImageModel(nodeParams.getModelId(), modelParamsSetting);
        List<String> answerTexts = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();
        int n = modelParamsSetting == null ? 1 : modelParamsSetting.getIntValue("n");
        n=n==0 ? 1 : n;
        Response<List<Image>> res = imageModel.generate(prompt,n);
        List<Image> images = res.content();
        for (Image image : images) {
            String imageMd ="!["+prompt+"](" + image.url() + ")";
            answerTexts.add(imageMd);
            imageUrls.add(image.url().toString());
        }
        if (nodeParams.getIsResult()){
            node.setAnswerText(String.join(" ",answerTexts));
        }
        node.getDetail().put("question",prompt);
        return new NodeResult(Map.of("answer",String.join(" ",answerTexts),"image",imageUrls));
    }
}
