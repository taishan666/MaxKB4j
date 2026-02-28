package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.AbsModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;

import java.util.List;

/**
 * LocalAI Model Provider - Local deployment
 */
public class LocalAIModelProvider extends AbsModelProvider {

    private static final String BASE_URL = "http://host.docker.internal:8080";


    @Override
    public List<ModelInfo> getModelList() {
        return List.of();
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BASE_URL);
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return LocalAiChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return LocalAiStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return LocalAiEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .maxRetries(getIntParam(params, "maxRetries"))
                .build();
    }
}
