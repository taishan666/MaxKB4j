package com.tarzan.maxkb4j.module.model.info.service;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@RequiredArgsConstructor
public class ModelFactory {

    private final ModelService modelService;


    public ChatModel buildChatModel(String modelId) {
        return buildChatModel(modelId, new JSONObject());
    }

    public ChatModel buildChatModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModel(modelId);
        IModelProvider modelProvider = getModelProvider(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildModelFallback(
                ModelType.LLM, model.getModelName(), model.getCredential(), modelParams,
                p -> modelProvider.buildChatModel(model.getModelName(), model.getCredential(), p));
    }

    public StreamingChatModel buildStreamingChatModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModel(modelId);
        IModelProvider modelProvider = getModelProvider(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildModelFallback(
                ModelType.LLM, model.getModelName(), model.getCredential(), modelParams,
                p -> modelProvider.buildStreamingChatModel(model.getModelName(), model.getCredential(), p));
    }

    public EmbeddingModel buildEmbeddingModel(String modelId) {
        return buildEmbeddingModel(modelId, new JSONObject());
    }

    public EmbeddingModel buildEmbeddingModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModel(modelId);
        IModelProvider modelProvider = getModelProvider(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildModelFallback(
                ModelType.EMBEDDING, model.getModelName(), model.getCredential(), modelParams,
                p -> modelProvider.buildEmbeddingModel(model.getModelName(), model.getCredential(), p));
    }

    public ImageModel buildImageModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModel(modelId);
        IModelProvider modelProvider = getModelProvider(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildModelFallback(
                ModelType.TTI, model.getModelName(), model.getCredential(), modelParams,
                p -> modelProvider.buildImageModel(model.getModelName(), model.getCredential(), p));
    }

    public ScoringModel buildScoringModel(String modelId) {
        return buildScoringModel(modelId, new JSONObject());
    }

    public ScoringModel buildScoringModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModel(modelId);
        IModelProvider modelProvider = getModelProvider(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildModelFallback(
                ModelType.RERANKER, model.getModelName(), model.getCredential(), modelParams,
                p -> modelProvider.buildScoringModel(model.getModelName(), model.getCredential(), p));
    }

    public TTSModel buildTTSModel(String modelId, JSONObject modelParams) {
        ModelEntity model = getModel(modelId);
        IModelProvider modelProvider = getModelProvider(model);
        modelParams = modelParams == null ? new JSONObject() : modelParams;
        return modelProvider.buildModelFallback(
                ModelType.TTS, model.getModelName(), model.getCredential(), modelParams,
                p -> modelProvider.buildTTSModel(model.getModelName(), model.getCredential(), p));
    }

    public STTModel buildSTTModel(String modelId) {
        ModelEntity model = getModel(modelId);
        IModelProvider modelProvider = getModelProvider(model);
        return modelProvider.buildModelFallback(
                ModelType.STT, model.getModelName(), model.getCredential(), new JSONObject(),
                p -> modelProvider.buildSTTModel(model.getModelName(), model.getCredential(), p));
    }


    public ModelEntity getModel(String modelId) {
        if (StringUtils.isBlank(modelId)) {
            return null;
        }
        return modelService.getCacheModelById(modelId);
    }

    public IModelProvider getModelProvider(ModelEntity model) {
        if (model == null) {
            return null;
        }
        return ModelProviderEnum.get(model.getProvider());
    }


}
