package com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.imagegenerate.input.ImageGenerateParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_GENERATE;

public class ImageGenerateNode extends INode {

    private final ModelService modelService;
    private final MongoFileService fileService;

    public ImageGenerateNode(JSONObject properties) {
        super(properties);
        this.type=IMAGE_GENERATE.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.fileService = SpringUtil.getBean(MongoFileService.class);
    }

    @Override
    public NodeResult execute() {
        ImageGenerateParams nodeParams=super.getNodeData().toJavaObject(ImageGenerateParams.class);
        String prompt=super.workflowManage.generatePrompt(nodeParams.getPrompt());
        String negativePrompt=nodeParams.getNegativePrompt();
        JSONObject modelParamsSetting=nodeParams.getModelParamsSetting();
        if (modelParamsSetting!=null){
            modelParamsSetting.put("negative_prompt",negativePrompt);
        }
        List<ChatFile> imageList=super.workflowManage.getChatParams().getImageList();
        StringBuilder answerSb=new StringBuilder();
        List<String> imageUrls = new ArrayList<>();
        ImageModel imageModel = modelService.getModelById(nodeParams.getModelId(), modelParamsSetting);
        if(!imageList.isEmpty()){
            ChatFile chatFile = imageList.get(0);
            byte[] bytes = fileService.getBytes(chatFile.getFileId());
            String base64Encoded = Base64.getEncoder().encodeToString(bytes);
            Image inputImage=Image.builder().base64Data(base64Encoded).mimeType(chatFile.getType()).build();
            Response<Image> res=imageModel.edit(inputImage,prompt);
            Image outImage=res.content();
            String imageMd ="!["+prompt+"](" + outImage.url() + ")";
            answerSb.append(" ").append(imageMd);
            imageUrls.add(outImage.url().toString());
        }else {
            int n=modelParamsSetting==null?1:modelParamsSetting.getIntValue("n");
            n=n==0 ? 1 : n;
            Response<List<Image>> res = imageModel.generate(prompt,n);
            List<Image> images = res.content();
            for (Image image : images) {
                String imageMd ="!["+prompt+"](" + image.url() + ")";
                answerSb.append(" ").append(imageMd);
                imageUrls.add(image.url().toString());
            }
        }
        return new NodeResult(Map.of("question",prompt,"answer",answerSb.toString(),"image",imageUrls),Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("question",context.get("question"));
        detail.put("answer",context.get("answer"));
        detail.put("messageTokens",0);
        detail.put("answerTokens",0);
        return detail;
    }

}
