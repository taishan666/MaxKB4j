package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.community.model.dashscope.WanxImageSize;
import dev.langchain4j.community.model.dashscope.WanxImageStyle;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

public class BaiLianImageModel implements ImageModel {

    private String modelName;
    private ModelCredential credential;
    private JSONObject params;
    private ImageModel instance;

    public BaiLianImageModel(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        this.params = params;
        this.instance = buildInstance(modelName);
    }

    private ImageModel buildInstance(String modelName) {
        if (modelName.startsWith("wanx-")||modelName.startsWith("wan2.")){
            return WanxImageModel.builder()
                    .modelName(modelName)
                    .apiKey(credential.getApiKey())
                    .size(WanxImageSize.of(params.getString("size")))
                    .style(WanxImageStyle.of(params.getString("style")))
                    .negativePrompt(params.getString("negative_prompt"))
                    .promptExtend(params.getBoolean("prompt_extend"))
                    .watermark(params.getBoolean("watermark"))
                    .seed(params.getInteger("seed"))
                    .build();
        }else {
            return new QwenImageModel(modelName, credential, params);
        }

    }

    @Override
    public Response<Image> generate(String prompt) {
        return instance.generate(prompt);
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        return instance.generate(prompt, n);
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        return instance.edit(image, prompt);
    }

    @Override
    public Response<Image> edit(Image image, Image mask, String prompt) {
        return instance.edit(image, mask, prompt);
    }
}
