package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

import java.util.List;

public class GeminiModelProvider extends IModelProvider {

    private final HttpClientBuilder httpClientBuilder = buildHttpClientBuilder();

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("gemini-1.0-pro", "", ModelType.LLM),
            new ModelInfo("gemini-1.0-pro-visio", "", ModelType.LLM),
            new ModelInfo("gemini-embedding-001", "", ModelType.EMBEDDING),
            new ModelInfo("gemini-1.5-flash", "", ModelType.VISION),
            new ModelInfo("gemini-1.5-pro", "", ModelType.VISION)
    );

    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.Gemini);
        info.setIcon(getSvgIcon("gemini_icon.svg"));
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return GoogleAiGeminiChatModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return GoogleAiEmbeddingModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }


}
