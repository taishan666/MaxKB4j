package com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

public class OpenaiEmbedding implements BaseModel<EmbeddingModel> {
    @Override
    public OpenAiEmbeddingModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  OpenAiEmbeddingModel.builder()
                //.baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
