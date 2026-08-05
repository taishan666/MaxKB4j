package com.maxkb4j.model.custom.model;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Optional;

public class SiliconFlowImageModel implements ImageModel {

    private final HttpRequest request;
    private final String modelName;
    private final JSONObject modelParams;

    public SiliconFlowImageModel(String modelName, ModelCredential credential, JSONObject modelParams) {
        HttpRequest request= HttpUtil.createRequest(Method.POST, credential.getBaseUrl()+"/images/generations");
        request.bearerAuth(credential.getApiKey());
        request.header("Content-Type", "application/json");
        this.request= request;
        this.modelName = modelName;
        this.modelParams = modelParams;
    }

    @Override
    public Response<Image> generate(String prompt) {
        Response<List<Image>> res = generate(prompt, 1);
        return Response.from(res.content().getFirst());
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        JSONObject params = new JSONObject();
        params.put("model",modelName);
        params.put("prompt",prompt);
        params.put("batch_size",n);
        String imageSize = modelParams.getString("size");
        if (imageSize != null){
            params.put("image_size",imageSize);
        }
        String negativePrompt = modelParams.getString("negative_prompt");
        if (negativePrompt != null){
            params.put("negative_prompt",negativePrompt);
        }
        HttpResponse response = request.body(params.toJSONString()).execute();
        if (response.isOk()){
            JSONObject responseBody = JSONObject.parseObject(response.body());
            JSONArray results = responseBody.getJSONArray("images");
            List<Image> images = Optional.ofNullable(results)
                    .orElse(new JSONArray())
                    .stream()
                    .filter(item -> item instanceof JSONObject) // 防御性类型过滤
                    .map(item -> {
                        String url =((JSONObject) item).getString("url");
                        return Image.builder()
                                .url(url)
                                .build();
                    })
                    .toList();
            return Response.from(images);
        }
        return Response.from(List.of());
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        JSONObject params = new JSONObject();
        params.put("model",modelName);
        params.put("prompt",prompt);
        params.put("batch_size",n);
        String imageSize = modelParams.getString("size");
        if (imageSize != null){
            params.put("image_size",imageSize);
        }
        String negativePrompt = modelParams.getString("negative_prompt");
        if (negativePrompt != null){
            params.put("negative_prompt",negativePrompt);
        }
        if (image != null){
            if (image.base64Data() != null){
                params.put("image",image.base64Data());
            }else if (image.url() != null){
                params.put("image",image.url().toString());
            }
        }
        HttpResponse response = request.body(params.toJSONString()).execute();
        if (response.isOk()){
            JSONObject responseBody = JSONObject.parseObject(response.body());
            JSONArray results = responseBody.getJSONArray("images");
            List<Image> images = Optional.ofNullable(results)
                    .orElse(new JSONArray())
                    .stream()
                    .filter(item -> item instanceof JSONObject) // 防御性类型过滤
                    .map(item -> {
                        String url =((JSONObject) item).getString("url");
                        return Image.builder()
                                .url(url)
                                .build();
                    })
                    .toList();
            return Response.from(images.getFirst());
        }
        return Response.from(Image.builder().build());
    }
}
