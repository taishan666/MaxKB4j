package com.tarzan.maxkb4j.module.model.provider.service.impl.xinferencemodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import dev.langchain4j.community.model.xinference.XinferenceEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class XinferenceEmbedding implements BaseModel<EmbeddingModel> {
    @Override
    public XinferenceEmbeddingModel build(String modelName, ModelCredential credential, JSONObject params) {
        return   XinferenceEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
