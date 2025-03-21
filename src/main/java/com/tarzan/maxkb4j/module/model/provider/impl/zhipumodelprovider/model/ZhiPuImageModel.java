package com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiImageModel;

public class ZhiPuImageModel implements BaseModel {

    @Override
    public <T> T build(String modelName, ModelCredential credential, JSONObject params) {
        return (T) ZhipuAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName).build();
    }




}
