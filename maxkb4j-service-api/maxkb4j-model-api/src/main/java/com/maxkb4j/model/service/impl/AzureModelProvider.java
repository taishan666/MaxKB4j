package com.maxkb4j.model.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.base.entity.ModelCredential;
import com.maxkb4j.model.custom.params.impl.LlmModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.AbsModelProvider;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.util.List;

/**
 * Azure OpenAI Model Provider
 */
public class AzureModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("Azure OpenAI", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("gpt-4", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("gpt-4o", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("gpt-4o-mini", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("text-embedding-3-large", "", ModelType.EMBEDDING),
            new ModelInfo("text-embedding-3-small", "", ModelType.EMBEDDING),
            new ModelInfo("text-embedding-ada-002", "", ModelType.EMBEDDING),
            new ModelInfo("gpt-4o", "", ModelType.VISION, new LlmModelParams()),
            new ModelInfo("gpt-4o-mini", "", ModelType.VISION, new LlmModelParams()),
            new ModelInfo("dall-e-3", "", ModelType.TTI)
    );


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiChatModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
    }
}
