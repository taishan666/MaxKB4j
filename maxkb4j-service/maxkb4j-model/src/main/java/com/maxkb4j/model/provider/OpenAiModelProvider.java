package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.custom.model.OpenAiSTTModel;
import com.maxkb4j.model.custom.model.OpenAiTTSModel;
import com.maxkb4j.model.custom.params.impl.OpenAiChatModelParams;
import com.maxkb4j.model.custom.params.impl.OpenAiImageModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.STTModel;
import com.maxkb4j.model.service.TTSModel;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.List;

/**
 * OpenAI Model Provider Implementation
 * Provides integration with OpenAI's API services
 */
public class OpenAiModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", ModelType.LLM),
            new ModelInfo("gpt-4", "GPT-4", ModelType.LLM),
            new ModelInfo("gpt-4o", "GPT-4 Omni", ModelType.LLM),
            new ModelInfo("gpt-4o-mini", "GPT-4 Omni Mini", ModelType.LLM),
            new ModelInfo("gpt-4-turbo", "GPT-4 Turbo", ModelType.LLM),
            new ModelInfo("gpt-4-turbo-preview", "GPT-4 Turbo Preview", ModelType.LLM),
            new ModelInfo("text-embedding-ada-002", "Text Embedding Ada v2", ModelType.EMBEDDING),
            new ModelInfo("whisper-1", "Whisper Speech-to-Text", ModelType.STT),
            new ModelInfo("tts-1", "Text-to-Speech", ModelType.TTS),
            new ModelInfo("gpt-4o", "GPT-4 Vision", ModelType.VISION),
            new ModelInfo("dall-e-2", "DALL·E 2", ModelType.TTI)
    );



    public String getDefaultBaseUrl(){
        return "https://api.openai.com/v1";
    }
    @Override
    public List<BaseField> getChatModelParamsForm() {
        return new OpenAiChatModelParams().toForm();
    }

    @Override
    public List<BaseField> getImageModelParamsForm() {
        return new OpenAiImageModelParams().toForm();
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, getDefaultBaseUrl());
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .sendThinking(getBooleanParam(params,"returnThinking"))
                .returnThinking(getBooleanParam(params,"returnThinking"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .sendThinking(getBooleanParam(params,"returnThinking"))
                .returnThinking(getBooleanParam(params,"returnThinking"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiImageModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .size(getStringParam(params, "size"))
                .quality(getStringParam(params, "quality"))
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
