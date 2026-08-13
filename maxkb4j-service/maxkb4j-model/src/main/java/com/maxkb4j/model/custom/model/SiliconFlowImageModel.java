package com.maxkb4j.model.custom.model;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * SiliconFlow 图像模型（OpenAI 兼容 /images/generations 接口）
 * <p>
 * 职责拆分：
 * <ul>
 *     <li>{@link #createRequest(ModelCredential)} —— 由凭证构建 HTTP 请求，与业务逻辑解耦</li>
 *     <li>{@link #buildBaseParams(String, int)} —— 组装公共请求体</li>
 *     <li>{@link #executeForImages(JSONObject)} —— 统一执行请求</li>
 *     <li>{@link #parseImages(String)} —— 纯函数式响应解析，便于单元测试</li>
 * </ul>
 */
@Slf4j
public class SiliconFlowImageModel implements ImageModel {

    private static final String IMAGES_GENERATIONS_PATH = "/images/generations";

    private static final String PARAM_MODEL = "model";
    private static final String PARAM_PROMPT = "prompt";
    private static final String PARAM_BATCH_SIZE = "batch_size";
    private static final String PARAM_IMAGE_SIZE = "image_size";
    private static final String PARAM_NEGATIVE_PROMPT = "negative_prompt";
    private static final String PARAM_IMAGE = "image";

    private static final String RESPONSE_IMAGES = "images";
    private static final String RESPONSE_URL = "url";

    private final HttpRequest request;
    private final String modelName;
    private final JSONObject modelParams;

    public SiliconFlowImageModel(String modelName, ModelCredential credential, JSONObject modelParams) {
        this(createRequest(credential), modelName, modelParams);
    }

    /**
     * 包级构造器：允许注入已构建好的请求，便于测试与替换传输实现
     */
    SiliconFlowImageModel(HttpRequest request, String modelName, JSONObject modelParams) {
        this.request = request;
        this.modelName = modelName;
        this.modelParams = modelParams;
    }

    private static HttpRequest createRequest(ModelCredential credential) {
        HttpRequest request = HttpUtil.createRequest(Method.POST, credential.getBaseUrl() + IMAGES_GENERATIONS_PATH);
        request.bearerAuth(credential.getApiKey());
        request.header("Content-Type", "application/json");
        return request;
    }

    @Override
    public Response<Image> generate(String prompt) {
        Response<List<Image>> res = generate(prompt, 1);
        return Response.from(res.content().getFirst());
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        JSONObject params = buildBaseParams(prompt, n);
        return Response.from(executeForImages(params));
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        JSONObject params = buildBaseParams(prompt, 1);
        appendImage(params, image);
        List<Image> images = executeForImages(params);
        if (images.isEmpty()) {
            return Response.from(Image.builder().build());
        }
        return Response.from(images.getFirst());
    }

    /**
     * 组装公共请求体：模型、提示词、批量数及可选参数
     */
    private JSONObject buildBaseParams(String prompt, int batchSize) {
        JSONObject params = new JSONObject();
        params.put(PARAM_MODEL, modelName);
        params.put(PARAM_PROMPT, prompt);
        params.put(PARAM_BATCH_SIZE, batchSize);
        String imageSize = modelParams.getString("size");
        if (imageSize != null) {
            params.put(PARAM_IMAGE_SIZE, imageSize);
        }
        String negativePrompt = modelParams.getString("negative_prompt");
        if (negativePrompt != null) {
            params.put(PARAM_NEGATIVE_PROMPT, negativePrompt);
        }
        return params;
    }

    /**
     * 编辑场景下附加原始图像：优先 base64 数据，其次 URL
     */
    private void appendImage(JSONObject params, Image image) {
        if (image == null) {
            return;
        }
        if (image.base64Data() != null) {
            params.put(PARAM_IMAGE, image.base64Data());
        } else if (image.url() != null) {
            params.put(PARAM_IMAGE, image.url().toString());
        }
    }

    /**
     * 执行请求并解析图像列表；请求失败时记录日志并返回空列表
     */
    private List<Image> executeForImages(JSONObject params) {
        try (HttpResponse response = request.body(params.toJSONString()).execute()) {
            if (response.isOk()) {
                return parseImages(response.body());
            }
            log.warn("SiliconFlow 图像接口调用失败, model={}, status={}, body={}",
                    modelName, response.getStatus(), response.body());
        }
        return List.of();
    }

    /**
     * 从响应体解析图像 URL 列表（纯函数，便于单元测试）
     */
    static List<Image> parseImages(String responseBody) {
        JSONObject body = JSONObject.parseObject(responseBody);
        if (body == null) {
            return List.of();
        }
        JSONArray results = body.getJSONArray(RESPONSE_IMAGES);
        return Optional.ofNullable(results)
                .orElse(new JSONArray())
                .stream()
                .filter(item -> item instanceof JSONObject) // 防御性类型过滤
                .map(item -> Image.builder()
                        .url(((JSONObject) item).getString(RESPONSE_URL))
                        .build())
                .toList();
    }
}
