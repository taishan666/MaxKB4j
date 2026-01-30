package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QwenImageModel implements ImageModel {

    private MultiModalConversationParam param;
    private Map<String, Object> parameters;

    public QwenImageModel(String modelName, ModelCredential credential, JSONObject params) {
        this.param = MultiModalConversationParam.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
        this.parameters = params;
    }

    @Override
    public Response<Image> generate(String prompt) {
        MultiModalConversation conv = new MultiModalConversation();
        Map<String, Object> localParams = new HashMap<>(this.parameters);
        localParams.put("n", 1);
        param.setParameters(localParams);
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(List.of(Map.of("text", prompt))).build();
        param.setMessages(Collections.singletonList(userMessage));
        try {
            MultiModalConversationResult result = conv.call(param);
            List<Map<String, Object>> contentList = result.getOutput().getChoices().get(0).getMessage().getContent();
            if (!contentList.isEmpty()) {
                Map<String, Object> content = contentList.get(0);
                if (content.containsKey("image")) {
                    System.out.println("输出图像的URL：" + content.get("image"));
                    return Response.from(Image.builder().url(content.get("image").toString()).build());
                }
            }
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException("图像生成失败: " + e.getMessage(), e);
        }
        return Response.from(Image.builder().build());
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        MultiModalConversation conv = new MultiModalConversation();
        Map<String, Object> localParams = new HashMap<>(this.parameters);
        localParams.put("n", n);
        param.setParameters(localParams);
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(List.of(Map.of("text", prompt))).build();
        param.setMessages(Collections.singletonList(userMessage));
        
        List<Image> images = new ArrayList<>();
        try {
            MultiModalConversationResult result = conv.call(param);
            List<Map<String, Object>> contentList = result.getOutput().getChoices().get(0).getMessage().getContent();
            for (Map<String, Object> content : contentList) {
                if (content.containsKey("image")) {
                    System.out.println("输出图像的URL：" + content.get("image"));
                    images.add(Image.builder().url(content.get("image").toString()).build());
                }
            }
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException("批量图像生成失败: " + e.getMessage(), e);
        }
        return Response.from(images);
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        MultiModalConversation conv = new MultiModalConversation();
        Map<String, Object> localParams = new HashMap<>(this.parameters);
        localParams.put("n", 1);
        param.setParameters(localParams);
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(List.of(Map.of("image", formatImageBase64(image)), Map.of("text", prompt))).build();
        param.setMessages(Collections.singletonList(userMessage));
        try {
            MultiModalConversationResult result = conv.call(param);
            List<Map<String, Object>> contentList = result.getOutput().getChoices().get(0).getMessage().getContent();
            if (!contentList.isEmpty()) {
                Map<String, Object> content = contentList.get(0);
                if (content.containsKey("image")) {
                    return Response.from(Image.builder().url(content.get("image").toString()).build());
                }
            }
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException("图像编辑失败: " + e.getMessage(), e);
        }
        return Response.from(Image.builder().build());
    }

    private String formatImageBase64(Image image) {
        return "data:"+image.mimeType()+";base64,"+image.base64Data();
    }

    @Override
    public Response<Image> edit(Image image, Image mask, String prompt) {
        MultiModalConversation conv = new MultiModalConversation();
        Map<String, Object> localParams = new HashMap<>(this.parameters);
        localParams.put("n", 1);
        param.setParameters(localParams);
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(List.of(Map.of("image", image.base64Data()), Map.of("image", mask.base64Data()), Map.of("text", prompt))).build();
        param.setMessages(Collections.singletonList(userMessage));
        try {
            MultiModalConversationResult result = conv.call(param);
            List<Map<String, Object>> contentList = result.getOutput().getChoices().get(0).getMessage().getContent();
            if (!contentList.isEmpty()) {
                Map<String, Object> content = contentList.get(0);
                if (content.containsKey("image")) {
                    System.out.println("输出图像的URL：" + content.get("image"));
                    return Response.from(Image.builder().url(content.get("image").toString()).build());
                }
            }
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException("图像蒙版编辑失败: " + e.getMessage(), e);
        }
        return Response.from(Image.builder().build());
    }
}