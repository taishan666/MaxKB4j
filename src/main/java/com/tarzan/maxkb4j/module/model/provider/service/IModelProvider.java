package com.tarzan.maxkb4j.module.model.provider.service;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.custom.credential.impl.BaseModelCredential;
import com.tarzan.maxkb4j.module.model.custom.model.disabled.DisabledSTTModel;
import com.tarzan.maxkb4j.module.model.custom.model.disabled.DisabledScoringModel;
import com.tarzan.maxkb4j.module.model.custom.model.disabled.DisabledTTSModel;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.custom.params.impl.NoModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.DisabledChatModel;
import dev.langchain4j.model.chat.DisabledStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.DisabledEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.DisabledImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

public abstract class IModelProvider {


    private ModelInfo getModelInfo(String modelType, String modelName) {
        List<ModelInfo> modelList = getModelList();
        if (modelList==null) {
            return null;
        }
        return modelList.stream().filter(modelInfo -> modelInfo.getModelType().getKey().equals(modelType) && modelInfo.getName().equals(modelName)).findFirst().orElse(null);
    }


    public BaseModelCredential getModelCredential() {
        return new BaseModelCredential(false, true);
    }


    private ModelParams getDefaultModelParams(String type) {
        ModelType modelType = ModelType.getByKey(type);
        assert modelType != null;
        return switch (modelType) {
            case LLM, TTI -> new LlmModelParams();
            default -> new NoModelParams();
        };
    }

    public ModelParams getModelParams(String modelType, String modelName) {
        ModelInfo modelInfo = this.getModelInfo(modelType, modelName);
        if (modelInfo == null) {
            return getDefaultModelParams(modelType);
        }
        return modelInfo.getModelParams();
    }

    public boolean isSupport(String modelType) {
        List<ModelInfo> modelList = getModelList();
        return modelList.stream().anyMatch(e -> e.getModelType().getKey().equals(modelType));
    }


    private Object buildModelFromInfo(String modelType, String modelName, ModelCredential credential, JSONObject params) {
        ModelInfo modelInfo = getModelInfo(modelType, modelName);
        if (modelInfo == null || modelInfo.getModelClass() == null) {
            return null;
        }
        Class<?> clazz = modelInfo.getModelClass();
        try {
            return clazz.getDeclaredConstructor(String.class, ModelCredential.class, JSONObject.class)
                    .newInstance(modelName, credential, params);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate model: " + modelName + " of type " + modelType, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T buildWithFallback(
            String modelType,
            String modelName,
            ModelCredential credential,
            JSONObject params,
            Function<JSONObject, T> fallbackBuilder) {
        Object model = buildModelFromInfo(modelType, modelName, credential, params);
        return model != null ? (T) model : fallbackBuilder.apply(params);
    }


    public abstract ModelProviderInfo getBaseInfo();

    public abstract List<ModelInfo> getModelList();

    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledChatModel();
    }

    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledStreamingChatModel();
    }

    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledEmbeddingModel();
    }

    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledImageModel();
    }

    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledScoringModel();
    }

    public STTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledSTTModel();
    }


    public TTSModel buildTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledTTSModel();
    }


}
