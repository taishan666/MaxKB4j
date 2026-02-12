package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;

import java.util.List;

public class LocalAIModelProvider extends IModelProvider {

    private final static String BASE_URL = "http://host.docker.internal:8080";

    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.LocalAI);
        info.setIcon(getSvgIcon("local_ai_icon.svg"));
        return info;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm( false,BASE_URL);
    }

    @Override
    public List<ModelInfo> getModelList() {
        return List.of();
    }


    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return LocalAiChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(params.getDouble("temperature"))
                .maxTokens(params.getInteger("maxTokens"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return LocalAiStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(params.getDouble("temperature"))
                .maxTokens(params.getInteger("maxTokens"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return LocalAiEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .maxRetries(params.getInteger("maxRetries"))
                .build();
    }



}
