package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.util.MimeTypeUtils;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ImageGenerateNode;
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
public class ImageGenerateNodeHandler extends AbstractNodeHandler<ImageGenerateNode.NodeParams> {

    private final IModelProviderService modelFactory;
    private final IOssService fileService;

    @Override
    protected Class<ImageGenerateNode.NodeParams> getParamsClass() {
        return ImageGenerateNode.NodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, ImageGenerateNode.NodeParams params) throws Exception {
        String prompt = workflow.renderPrompt(params.getPrompt());
        String negativePrompt = params.getNegativePrompt();
        JSONObject modelParamsSetting = params.getModelParamsSetting();

        if (modelParamsSetting != null) {
            modelParamsSetting.put("negative_prompt", negativePrompt);
        }

        List<String> answerTexts = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();
        ImageModel imageModel = modelFactory.buildImageModel(params.getModelId(), modelParamsSetting);
        List<String> imageFieldList = params.getImageList();
        List<Image> outImages = new ArrayList<>();
        List<Image> editImages = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(imageFieldList)) {
            editImages = buildImages(workflow, node, imageFieldList);
        }

        if (CollectionUtils.isNotEmpty(editImages)) {
            for (Image editImage : editImages) {
                Response<Image> res = imageModel.edit(editImage, prompt);
                outImages.add(res.content());
            }
        } else {
            int n = modelParamsSetting == null ? 1 : modelParamsSetting.getIntValue("n");
            n = n == 0 ? 1 : n;
            Response<List<Image>> res = imageModel.generate(prompt, n);
            outImages = res.content();
        }

        for (Image image : outImages) {
            String imageMd = "![" + prompt + "](" + image.url() + ")";
            answerTexts.add(imageMd);
            imageUrls.add(image.url().toString());
        }

        if (params.getIsResult()) {
            setAnswer(node, String.join(" ", answerTexts));
        }

        putDetail(node, "question", prompt);

        return new NodeResult(Map.of("answer", String.join(" ", answerTexts), "image", imageUrls));
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
            List<OssFile> imageFiles = fileList.stream()
                    .filter(OssFile.class::isInstance)
                    .map(OssFile.class::cast)
                    .toList();
            putDetail(node, "imageList", imageFiles);

            for (OssFile file : imageFiles) {
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
