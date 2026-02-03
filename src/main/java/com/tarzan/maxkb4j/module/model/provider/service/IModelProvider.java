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
import java.util.List;

/**
 * Abstract base class for model providers
 * Defines the contract for all model providers in the system
 */
public abstract class IModelProvider {

    /**
     * Checks if the provider supports a specific model type
     * @param modelType the model type to check
     * @return true if supported, false otherwise
     */
    public boolean isSupport(ModelType modelType) {
        return getModelList().stream().anyMatch(e -> e.getModelType().equals(modelType));
    }

    /**
     * Gets model info for a specific model type and name
     * @param modelType the model type
     * @param modelName the model name
     * @return the model info or null if not found
     */
    public ModelInfo getModelInfo(ModelType modelType, String modelName) {
        return getModelList().stream()
                .filter(modelInfo ->
                    modelInfo.getModelType().equals(modelType) &&
                    modelInfo.getName().equals(modelName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the model credential form configuration
     * @return the credential form configuration
     */
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(false, true);
    }

    /**
     * Gets SVG icon for the provider
     * @param name the icon name
     * @return the SVG icon as string
     */
    public String getSvgIcon(String name) {
        ClassLoader classLoader = IModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("model-icons/" + name);
        return IoUtil.readToString(inputStream);
    }


    /**
     * Gets the base information for this provider
     * @return the provider info
     */
    public abstract ModelProviderInfo getBaseInfo();

    /**
     * Gets the list of available models for this provider
     * @return list of model info
     */
    public abstract List<ModelInfo> getModelList();


    /**
     * Builds a chat model instance
     * @param modelName the model name
     * @param credential the model credentials
     * @param params additional parameters
     * @return the chat model instance
     */
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledChatModel();
    }

    /**
     * Builds a streaming chat model instance
     * @param modelName the model name
     * @param credential the model credentials
     * @param params additional parameters
     * @return the streaming chat model instance
     */
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledStreamingChatModel();
    }

    /**
     * Builds an embedding model instance
     * @param modelName the model name
     * @param credential the model credentials
     * @param params additional parameters
     * @return the embedding model instance
     */
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledEmbeddingModel();
    }

    /**
     * Builds an image model instance
     * @param modelName the model name
     * @param credential the model credentials
     * @param params additional parameters
     * @return the image model instance
     */
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledImageModel();
    }

    /**
     * Builds a scoring model instance
     * @param modelName the model name
     * @param credential the model credentials
     * @param params additional parameters
     * @return the scoring model instance
     */
    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledScoringModel();
    }

    /**
     * Builds an STT (speech-to-text) model instance
     * @param modelName the model name
     * @param credential the model credentials
     * @param params additional parameters
     * @return the STT model instance
     */
    public STTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledSTTModel();
    }

    /**
     * Builds a TTS (text-to-speech) model instance
     * @param modelName the model name
     * @param credential the model credentials
     * @param params additional parameters
     * @return the TTS model instance
     */
    public TTSModel buildTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        return new DisabledTTSModel();
    }

}
