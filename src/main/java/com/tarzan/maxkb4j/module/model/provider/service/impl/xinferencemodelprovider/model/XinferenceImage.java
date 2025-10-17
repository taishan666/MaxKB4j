package com.tarzan.maxkb4j.module.model.provider.service.impl.xinferencemodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import dev.langchain4j.community.model.xinference.XinferenceImageModel;
import dev.langchain4j.model.image.ImageModel;

public class XinferenceImage implements BaseModel<ImageModel> {
    @Override
    public ImageModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  XinferenceImageModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
