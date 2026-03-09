package com.maxkb4j.model.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.base.entity.ModelCredential;
import com.maxkb4j.model.custom.base.STTModel;
import com.maxkb4j.model.custom.base.TTSModel;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.custom.model.OpenAiSTTModel;
import com.maxkb4j.model.custom.model.OpenAiTTSModel;
import com.maxkb4j.model.custom.params.impl.LlmModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.AbsModelProvider;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.community.model.xinference.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;

import java.util.List;

/**
 * XInference Model Provider - Local deployment with OpenAI compatible API
 */
public class XInferenceModelProvider extends AbsModelProvider {

    private static final String BASE_URL = "http://host.docker.internal:9997";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("qwen3:8b", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("bge-m3", "", ModelType.EMBEDDING, new LlmModelParams()),
            new ModelInfo("llava:7b", "", ModelType.VISION, new LlmModelParams()),
            new ModelInfo("sdxl-turbo", "", ModelType.TTI, new LlmModelParams()),
            new ModelInfo("bge-reranker-base", "", ModelType.RERANKER),
            new ModelInfo("ChatTTS", "", ModelType.TTS),
            new ModelInfo("whisper-large-v3", "", ModelType.STT)
    );


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BASE_URL);
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceImageModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceScoringModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public STTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new OpenAiSTTModel(modelName, credential, params);
    }

    @Override
    public TTSModel buildTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        return new OpenAiTTSModel(modelName, credential, params);
    }
}
