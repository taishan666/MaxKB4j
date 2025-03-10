package com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.xinference.XinferenceEmbeddingModel;

public class XinferenceEmbedding implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) XinferenceEmbeddingModel.builder()
                .baseUrl(credential.getApiBase())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
