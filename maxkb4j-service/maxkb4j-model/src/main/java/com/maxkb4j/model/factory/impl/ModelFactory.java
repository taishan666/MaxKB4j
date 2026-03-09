package com.maxkb4j.model.factory.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.custom.base.STTModel;
import com.maxkb4j.model.custom.base.TTSModel;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.enums.ModelProvider;
import com.maxkb4j.model.exception.ModelNotFoundException;
import com.maxkb4j.model.service.AbsModelProvider;
import com.maxkb4j.model.service.IModelFactory;
import com.maxkb4j.model.service.ModelService;
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
public class ModelFactory implements IModelFactory {

    private final ModelService modelService;

    @Override
    public ChatModel buildChatModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildChatModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public ChatModel buildChatModel(String modelId) {
        return buildChatModel(modelId, new JSONObject());
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
    public EmbeddingModel buildEmbeddingModel(String modelId) {
        return buildEmbeddingModel(modelId, new JSONObject());
    }

    @Override
    public ImageModel buildImageModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildImageModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public ScoringModel buildScoringModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildScoringModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public ScoringModel buildScoringModel(String modelId) {
        return buildScoringModel(modelId, new JSONObject());
    }

    @Override
    public TTSModel buildTTSModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildTTSModel(model.getModelName(), model.getCredential(), modelParams);
    }

    @Override
    public STTModel buildSTTModel(String modelId) {
        ModelEntity model = getModelOrThrow(modelId);
        AbsModelProvider modelProvider = getModelProviderOrThrow(model);
        return modelProvider.buildSTTModel(model.getModelName(), model.getCredential(), new JSONObject());
    }


    public ModelEntity getModel(String modelId) {
        if (StringUtils.isBlank(modelId)) {
            return null;
        }
        return modelService.lambdaQuery()
                .select(ModelEntity::getProvider, ModelEntity::getModelType, ModelEntity::getModelName, ModelEntity::getCredential)
                .eq(ModelEntity::getId, modelId)
                .one();
    }

    private ModelEntity getModelOrThrow(String modelId) {
        if (StringUtils.isBlank(modelId)) {
            throw new IllegalArgumentException("Model ID cannot be blank");
        }
        ModelEntity model = getModel(modelId);
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
        return ModelProvider.get(model.getProvider());
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
