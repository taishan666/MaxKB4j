package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.custom.model.OpenAiSTTModel;
import com.tarzan.maxkb4j.module.model.custom.model.OpenAiTTSModel;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Model Provider Implementation
 * Provides integration with OpenAI's API services
 */
public class OpenAiModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.OpenAI);
        info.setIcon(getSvgIcon("openai_icon.svg"));
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4", "GPT-4", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o", "GPT-4 Omni", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o-mini", "GPT-4 Omni Mini", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4-turbo", "GPT-4 Turbo", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4-turbo-preview", "GPT-4 Turbo Preview", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-ada-002", "Text Embedding Ada v2", ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("whisper-1", "Whisper Speech-to-Text", ModelType.STT));
        modelInfos.add(new ModelInfo("tts-1", "Text-to-Speech", ModelType.TTS));
        modelInfos.add(new ModelInfo("gpt-4o", "GPT-4 Vision", ModelType.VISION, new LlmModelParams()));
        modelInfos.add(new ModelInfo("dall-e-2", "DALL·E 2", ModelType.TTI));
        return modelInfos;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, true); // Both API key and base URL required
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(params.getDouble("temperature"))
                .topP(params.getDouble("topP"))
                .maxTokens(params.getInteger("maxTokens"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(params.getDouble("temperature"))
                .topP(params.getDouble("topP"))
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
                .size(params.getString("size"))
                .quality(params.getString("quality"))
                .style(params.getString("style"))
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
