package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.AbsModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiImageModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.util.List;

/**
 * ZhiPu (GLM) Model Provider
 */
public class ZhiPuModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("glm-4", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("glm-4v", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("glm-3-turbo", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("embedding-3", "", ModelType.EMBEDDING),
            new ModelInfo("glm-4v-plus", "", ModelType.VISION),
            new ModelInfo("glm-4v", "", ModelType.VISION, new LlmModelParams()),
            new ModelInfo("glm-4v-flash", "", ModelType.VISION, new LlmModelParams()),
            new ModelInfo("cogview-3", "", ModelType.TTI),
            new ModelInfo("cogview-3-plus", "", ModelType.TTI),
            new ModelInfo("cogview-3-flash", "", ModelType.TTI)
    );

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return ZhipuAiChatModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxToken(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return ZhipuAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxToken(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return ZhipuAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return ZhipuAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
    }
}
