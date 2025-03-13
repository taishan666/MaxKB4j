package com.tarzan.maxkb4j.module.model.provider.impl.azuremodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;

public class AzureOpenaiEmbedding implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) AzureOpenAiEmbeddingModel.builder()
                //.baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
    }
}
