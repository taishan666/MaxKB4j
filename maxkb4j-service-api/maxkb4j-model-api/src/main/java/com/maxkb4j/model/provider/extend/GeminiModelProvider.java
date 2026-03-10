package com.maxkb4j.model.provider.extend;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.base.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

import java.util.List;

/**
 * Google Gemini Model Provider
 */
public class GeminiModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("gemini-1.0-pro", "", ModelType.LLM),
            new ModelInfo("gemini-1.0-pro-visio", "", ModelType.LLM),
            new ModelInfo("gemini-embedding-001", "", ModelType.EMBEDDING),
            new ModelInfo("gemini-1.5-flash", "", ModelType.VISION),
            new ModelInfo("gemini-1.5-pro", "", ModelType.VISION)
    );


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return GoogleAiGeminiChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(getIntParam(params, "maxTokens"))
                .temperature(getDoubleParam(params, "temperature"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(getIntParam(params, "maxTokens"))
                .temperature(getDoubleParam(params, "temperature"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return GoogleAiEmbeddingModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
