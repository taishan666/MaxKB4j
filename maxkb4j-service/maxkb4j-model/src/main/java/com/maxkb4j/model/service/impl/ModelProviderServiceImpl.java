package com.maxkb4j.model.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.registry.ModelProviderRegistry;
import com.maxkb4j.model.exception.ModelNotFoundException;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.service.IModelInternalService;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.model.service.ISTTModel;
import com.maxkb4j.model.service.ITTSModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Model Factory - Creates model instances based on model ID
 * Implements caching for performance optimization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelProviderServiceImpl implements IModelProviderService {

    private final IModelInternalService modelService;
    private final ModelProviderRegistry providerRegistry;

    @Override
    public ChatModel buildChatModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildChatModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildStreamingChatModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildEmbeddingModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public ImageModel buildImageModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildImageModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public ScoringModel buildScoringModel(String modelId) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        return modelProvider.buildScoringModel(model.getModelName(), model.getCredential(), new JSONObject());
    }

    @Override
    public ITTSModel buildTTSModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildTTSModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public ISTTModel buildSTTModel(String modelId,JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        return modelProvider.buildSTTModel(model.getModelName(), model.getCredential(),modelParams);
    }

    private ModelEntity getModelOrThrow(String modelId) {
        if (StringUtils.isBlank(modelId)) {
            throw new IllegalArgumentException("Model ID cannot be blank");
        }
        ModelEntity model = modelService.getModelById(modelId);
        if (model == null) {
            log.error("Model not found with ID: {}", modelId);
            throw new ModelNotFoundException(modelId);
        }
        return model;
    }

    public AbsModelProvider getModelProvider(ModelEntity model) {
        if (model == null) {
            return null;
        }
        return providerRegistry.get(model.getProvider());
    }

    private AbsModelProvider getModelProviderOrThrow(ModelEntity model) {
        AbsModelProvider provider = getModelProvider(model);
        if (provider == null) {
            log.error("No model provider found for provider: {}", model.getProvider());
            throw new IllegalStateException("No model provider found for: " + model.getProvider());
        }
        return provider;
    }
}
