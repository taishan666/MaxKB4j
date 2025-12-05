package com.tarzan.maxkb4j.module.model.provider.service;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.custom.model.disabled.DisabledSTTModel;
import com.tarzan.maxkb4j.module.model.custom.model.disabled.DisabledScoringModel;
import com.tarzan.maxkb4j.module.model.custom.model.disabled.DisabledTTSModel;
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

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

public abstract class IModelProvider {


    public boolean isSupport(ModelType modelType) {
        return getModelList().stream().anyMatch(e -> e.getModelType().equals(modelType));
    }

    public ModelInfo getModelInfo(ModelType modelType, String modelName) {
        return getModelList().stream().filter(modelInfo -> modelInfo.getModelType().equals(modelType) && modelInfo.getName().equals(modelName)).findFirst().orElse(null);
    }

    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(false, true);
    }

    public String getSvgIcon(String name) {
        ClassLoader classLoader = IModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/"+name);
        return IoUtil.readToString(inputStream);
    }


    private Object buildModelClass(ModelType modelType, String modelName, ModelCredential credential, JSONObject params) {
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
    public <T> T buildModelFallback(ModelType modelType, String modelName, ModelCredential credential, JSONObject params, Function<JSONObject, T> fallback) {
        Object model = buildModelClass(modelType, modelName, credential, params);
        return model != null ? (T) model : fallback.apply(params);
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
