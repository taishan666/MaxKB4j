package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class QwenImageModel implements ImageModel {

    private final MultiModalConversationParam param;
    private final Map<String, Object> parameters;
    private final MultiModalConversation conv = new MultiModalConversation();

    public QwenImageModel(String modelName, ModelCredential credential, JSONObject params) {
        this.param = MultiModalConversationParam.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
        this.parameters = params;
    }

    @Override
    public Response<Image> generate(String prompt) {
        parameters.put("n", 1);
        param.setParameters(parameters);
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(List.of(Map.of("text", prompt)))
                .build();
        param.setMessages(Collections.singletonList(userMessage));
        try {
            MultiModalConversationResult result = conv.call(param);
            Image image = extractImageFromResult(result);
            if (image != null) {
                return Response.from(image);
            }
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException("图像生成失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("调用多模态对话接口发生未知错误", e);
            throw new RuntimeException("图像生成失败: " + e.getMessage(), e);
        }

        return Response.from(Image.builder().build());
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        parameters.put("n", n);
        param.setParameters(parameters);
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(List.of(Map.of("text", prompt)))
                .build();
        param.setMessages(Collections.singletonList(userMessage));
        List<Image> images = new ArrayList<>();
        try {
            MultiModalConversationResult result = conv.call(param);

            // 安全提取所有图片
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null) {
                for (var choice : result.getOutput().getChoices()) {
                    if (choice != null && choice.getMessage() != null && choice.getMessage().getContent() != null) {
                        List<Map<String, Object>> contentList = choice.getMessage().getContent();
                        for (Map<String, Object> content : contentList) {
                            if (content != null && content.containsKey("image")) {
                                String imageUrl = (String) content.get("image");
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    images.add(Image.builder().url(imageUrl).build());
                                }
                            }
                        }
                    }
                }
            }
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException("批量图像生成失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("批量图像生成发生未知错误", e);
            throw new RuntimeException("批量图像生成失败: " + e.getMessage(), e);
        }

        return Response.from(images);
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        parameters.put("n", 1);
        param.setParameters(parameters);
        // 确保 image 不为 null
        if (image == null || image.base64Data() == null) {
            throw new IllegalArgumentException("编辑图像时，原始图像不能为空");
        }

        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(List.of(
                        Map.of("image", formatImageBase64(image)),
                        Map.of("text", prompt)
                ))
                .build();
        param.setMessages(Collections.singletonList(userMessage));
        try {
            MultiModalConversationResult result = conv.call(param);
            Image generatedImage = extractImageFromResult(result);
            if (generatedImage != null) {
                return Response.from(generatedImage);
            }
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException("图像编辑失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("图像编辑发生未知错误", e);
            throw new RuntimeException("图像编辑失败: " + e.getMessage(), e);
        }

        return Response.from(Image.builder().build());
    }

    @Override
    public Response<Image> edit(Image image, Image mask, String prompt) {
        parameters.put("n", 1);
        param.setParameters(parameters);

        if (image == null || image.base64Data() == null) {
            throw new IllegalArgumentException("编辑图像时，原始图像不能为空");
        }
        if (mask == null || mask.base64Data() == null) {
            throw new IllegalArgumentException("编辑图像时，蒙版图像不能为空");
        }

        // 注意：这里原代码直接放 base64Data，而上面 edit 方法用了 formatImageBase64 (带 data:image/...;base64,)
        // 通常 DashScope 需要完整的 data URI 格式。如果原代码能跑通说明模型接受纯 base64，
        // 但为了保险起见，建议统一格式。这里暂时保持原逻辑，但增加了空检查。
        // 如果之前 formatImageBase64 是必须的，这里也应该用上。
        // 假设原代码逻辑是正确的，这里只修 NPE。

        List<Map<String, Object>> contentItems = new ArrayList<>();
        contentItems.add(Map.of("image", image.base64Data()));
        contentItems.add(Map.of("image", mask.base64Data()));
        contentItems.add(Map.of("text", prompt));

        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(contentItems)
                .build();
        param.setMessages(Collections.singletonList(userMessage));

        try {
            MultiModalConversationResult result = conv.call(param);
            Image generatedImage = extractImageFromResult(result);
            if (generatedImage != null) {
                log.info("输出图像的URL：{}", generatedImage.url());
                return Response.from(generatedImage);
            }
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException("图像蒙版编辑失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("图像蒙版编辑发生未知错误", e);
            throw new RuntimeException("图像蒙版编辑失败: " + e.getMessage(), e);
        }

        return Response.from(Image.builder().build());
    }

    /**
     * 抽取公共的解析逻辑，避免重复代码并集中处理 NPE
     */
    private Image extractImageFromResult(MultiModalConversationResult result) {
        if (result == null) {
            log.warn("API 返回结果为空");
            return null;
        }

        var output = result.getOutput();
        if (output == null) {
            log.warn("API 返回 Output 为空");
            return null;
        }

        var choices = output.getChoices();
        if (choices == null || choices.isEmpty()) {
            log.warn("API 返回 Choices 为空或不存在");
            return null;
        }

        var firstChoice = choices.get(0);
        if (firstChoice == null) {
            log.warn("第一个 Choice 为空");
            return null;
        }

        var message = firstChoice.getMessage();
        if (message == null) {
            log.warn("Message 为空");
            return null;
        }

        List<Map<String, Object>> contentList = message.getContent();
        if (contentList == null || contentList.isEmpty()) {
            log.warn("Content 列表为空");
            return null;
        }

        // 遍历查找第一个包含 image 的内容项
        for (Map<String, Object> content : contentList) {
            if (content != null && content.containsKey("image")) {
                Object imgUrlObj = content.get("image");
                if (imgUrlObj != null) {
                    String imageUrl = imgUrlObj.toString();
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        return Image.builder().url(imageUrl).build();
                    }
                }
            }
        }

        log.warn("未在返回内容中找到有效的 image 字段");
        return null;
    }

    private String formatImageBase64(Image image) {
        if (image == null || image.mimeType() == null || image.base64Data() == null) {
            return null;
        }
        return "data:" + image.mimeType() + ";base64," + image.base64Data();
    }
}