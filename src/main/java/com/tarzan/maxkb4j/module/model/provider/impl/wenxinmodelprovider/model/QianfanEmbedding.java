package com.tarzan.maxkb4j.module.model.provider.impl.wenxinmodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.qianfan.QianfanEmbeddingModel;

public class QianfanEmbedding implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) QianfanEmbeddingModel.builder()
                //.baseUrl(credential.getApiBase())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
