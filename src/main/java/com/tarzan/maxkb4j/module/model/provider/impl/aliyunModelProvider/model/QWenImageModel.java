package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;

public class QWenImageModel implements BaseModel {

    @Override
    public <T> T build(String modelName, ModelCredential credential, JSONObject params) {
        return (T) WanxImageModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName).build();
    }




}
