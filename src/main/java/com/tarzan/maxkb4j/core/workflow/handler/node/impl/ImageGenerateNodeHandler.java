package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.MimeTypeUtils;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ImageGenerateNode;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.springframework.web.util.UriUtils.extractFileExtension;

@NodeHandlerType(NodeType.IMAGE_GENERATE)
@RequiredArgsConstructor
@Component
@Slf4j
public class ImageGenerateNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    private final MongoFileService fileService;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        ImageGenerateNode.NodeParams nodeParams=node.getNodeData().toJavaObject(ImageGenerateNode.NodeParams.class);
        String prompt=workflow.generatePrompt(nodeParams.getPrompt());
        String negativePrompt=nodeParams.getNegativePrompt();
        JSONObject modelParamsSetting=nodeParams.getModelParamsSetting();
        if (modelParamsSetting!=null){
            modelParamsSetting.put("negative_prompt",negativePrompt);
        }
        List<String> answerTexts = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();
        ImageModel imageModel = modelFactory.buildImageModel(nodeParams.getModelId(), modelParamsSetting);
        List<String> imageFieldList=nodeParams.getImageList();
        List<Image> outImages = new ArrayList<>();
        List<Image> editImages=new ArrayList<>();
        if (CollectionUtils.isNotEmpty(imageFieldList)){
            editImages= buildImages(workflow, node, imageFieldList);
        }
        if (CollectionUtils.isNotEmpty(editImages)){
            for (Image editImage : editImages) {
                Response<Image>  res =  imageModel.edit(editImage,prompt);
                outImages.add(res.content());
            }
        }else {
            int n = modelParamsSetting == null ? 1 : modelParamsSetting.getIntValue("n");
            n=n==0 ? 1 : n;
            Response<List<Image>> res = imageModel.generate(prompt,n);
            outImages = res.content();
        }
        for (Image image : outImages) {
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

    private List<Image> buildImages(Workflow workflow, AbsNode node, List<String> imageFieldList) {
        List<Image> images = new ArrayList<>();
        if (CollectionUtils.isEmpty(imageFieldList)) {
            return images;
        }
        try {
            Object object = workflow.getReferenceField(imageFieldList);
            if (!(object instanceof List<?> fileList)) {
                return images;
            }
            List<SysFile> imageFiles = fileList.stream()
                    .filter(SysFile.class::isInstance)
                    .map(SysFile.class::cast)
                    .toList();
            node.getDetail().put("imageList", imageFiles);
            for (SysFile file : imageFiles) {
                byte[] bytes = fileService.getBytes(file.getFileId());
                String base64Data = Base64.getEncoder().encodeToString(bytes);
                String extension = extractFileExtension(file.getName());
                Image image = Image.builder().base64Data(base64Data).mimeType(MimeTypeUtils.getMimeType(extension)).build();
                images.add(image);
            }
        } catch (Exception e) {
            log.warn("Failed to load image contents for node: {}", node.getRuntimeNodeId(), e);
        }
        return images;
    }
}
