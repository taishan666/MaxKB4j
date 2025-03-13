package com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;

public class ZhiPuEmbedding implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) ZhipuAiEmbeddingModel.builder()
                //.baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
    }
}
