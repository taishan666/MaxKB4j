package com.tarzan.maxkb4j.module.model.provider.service.impl.azuremodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class AzureOpenaiEmbedding implements BaseModel<EmbeddingModel> {
    @Override
    public AzureOpenAiEmbeddingModel build(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiEmbeddingModel.builder()
                //.baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
    }
}
