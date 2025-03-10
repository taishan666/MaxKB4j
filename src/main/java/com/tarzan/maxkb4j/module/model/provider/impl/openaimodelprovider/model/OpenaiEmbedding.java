package com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

public class OpenaiEmbedding implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) OpenAiEmbeddingModel.builder()
                //.baseUrl(credential.getApiBase())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
