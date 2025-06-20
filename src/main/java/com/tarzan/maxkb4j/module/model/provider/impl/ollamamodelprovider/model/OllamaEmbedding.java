package com.tarzan.maxkb4j.module.model.provider.impl.ollamamodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

public class OllamaEmbedding implements BaseModel<EmbeddingModel> {
    @Override
    public OllamaEmbeddingModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  OllamaEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
    }
}
