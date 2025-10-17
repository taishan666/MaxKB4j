package com.tarzan.maxkb4j.module.model.provider.service.impl.zhipumodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiImageModel;
import dev.langchain4j.model.image.ImageModel;

public class ZhiPuImageModel implements BaseModel<ImageModel> {

    @Override
    public ImageModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  ZhipuAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName).build();
    }




}
