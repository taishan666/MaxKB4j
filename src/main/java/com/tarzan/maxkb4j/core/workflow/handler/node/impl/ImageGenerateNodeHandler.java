package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.tool.MimeTypeUtils;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ImageGenerateNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Component
public class ImageGenerateNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    private final MongoFileService fileService;

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        ImageGenerateNode.NodeParams nodeParams=node.getNodeData().toJavaObject(ImageGenerateNode.NodeParams.class);
        String prompt=workflow.generatePrompt(nodeParams.getPrompt());
        String negativePrompt=nodeParams.getNegativePrompt();
        JSONObject modelParamsSetting=nodeParams.getModelParamsSetting();
        if (modelParamsSetting!=null){
            modelParamsSetting.put("negativePrompt",negativePrompt);
        }
        ImageModel imageModel = modelFactory.build(nodeParams.getModelId(), modelParamsSetting);
        List<String> imageFieldList = nodeParams.getImageList();
        StringBuilder answerSb=new StringBuilder();
        List<String> imageUrls = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(imageFieldList)){
            Object object = workflow.getReferenceField(imageFieldList.get(0), imageFieldList.get(1));
            @SuppressWarnings("unchecked")
            List<ChatFile> imageFiles = (List<ChatFile>) object;
            ChatFile file = imageFiles.get(0);
            byte[] bytes = fileService.getBytes(file.getFileId());
            String base64Data = Base64.getEncoder().encodeToString(bytes);
            String fileName=file.getName();
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            Image editimage=Image.builder()
                    .base64Data(base64Data)
                    .mimeType(MimeTypeUtils.getMimeType(extension))
                    .revisedPrompt(prompt)
                    .build();
            Response<Image> res = imageModel.edit(editimage,prompt);
            Image image = res.content();
            String imageMd ="!["+prompt+"](" + image.url() + ")";
            answerSb.append(" ").append(imageMd);
            imageUrls.add(image.url().toString());
        }else {
            int n = modelParamsSetting == null ? 1 : modelParamsSetting.getIntValue("n");
            n=n==0 ? 1 : n;
            Response<List<Image>> res = imageModel.generate(prompt,n);
            List<Image> images = res.content();
            for (Image image : images) {
                String imageMd ="!["+prompt+"](" + image.url() + ")";
                answerSb.append(" ").append(imageMd);
                imageUrls.add(image.url().toString());
            }
        }
        node.getDetail().put("question",prompt);
        return new NodeResult(Map.of("answer",answerSb.toString(),"image",imageUrls),Map.of());
    }
}
