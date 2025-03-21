package com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.xinference.XinferenceEmbeddingModel;

public class XinferenceEmbedding implements BaseModel {
    @Override
    public <T> T build(String modelName, ModelCredential credential, JSONObject params) {
        return (T) XinferenceEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
