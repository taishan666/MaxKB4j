package com.tarzan.maxkb4j.module.model.provider.impl.ollamamodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

public class OllamaEmbedding implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) OllamaEmbeddingModel.builder()
                .baseUrl(credential.getApiBase())
                .modelName(modelName)
                .build();
    }
}
