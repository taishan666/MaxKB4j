package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.custom.disabled.DisabledSTTModel;
import com.maxkb4j.model.custom.disabled.DisabledScoringModel;
import com.maxkb4j.model.custom.disabled.DisabledTTSModel;
import com.maxkb4j.model.custom.params.impl.LLMChatModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.STTModel;
import com.maxkb4j.model.service.TTSModel;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.DisabledChatModel;
import dev.langchain4j.model.chat.DisabledStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.DisabledEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.DisabledImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for model providers
 * Defines the contract for all model providers in the system
 */
public abstract class AbsModelProvider {

    private volatile HttpClientBuilder httpClientBuilder;

    protected AbsModelProvider() {
        // 延迟初始化 HTTP 客户端，避免构造函数中耗时操作
    }

    protected HttpClientBuilder getHttpClientBuilder() {
        if (httpClientBuilder == null) {
            synchronized (this) {
                if (httpClientBuilder == null) {
                    httpClientBuilder = buildHttpClientBuilder();
                }
            }
        }
        return httpClientBuilder;
    }

    protected HttpClientBuilder buildHttpClientBuilder() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory());
        return SpringRestClient.builder()
                .restClientBuilder(restClientBuilder);
    }

    /**
     * Gets double value from params with null safety
     * @param params the params JSONObject
     * @param key the key to lookup
     * @return the double value or null if not present
     */
    protected Double getDoubleParam(JSONObject params, String key) {
        return Optional.ofNullable(params).map(p -> p.getDouble(key)).orElse(null);
    }

    /**
     * Gets integer value from params with null safety
     * @param params the params JSONObject
     * @param key the key to lookup
     * @return the integer value or null if not present
     */
    protected Integer getIntParam(JSONObject params, String key) {
        return Optional.ofNullable(params).map(p -> p.getInteger(key)).orElse(null);
    }

    /**
     * Gets string value from params with null safety
     * @param params the params JSONObject
     * @param key the key to lookup
     * @return the string value or null if not present
     */
    protected String getStringParam(JSONObject params, String key) {
        return Optional.ofNullable(params).map(p -> p.getString(key)).orElse(null);
    }

    /**
     * Gets boolean value from params with null safety
     * @param params the params JSONObject
     * @param key the key to lookup
     * @return the boolean value or null if not present
     */
    protected Boolean getBooleanParam(JSONObject params, String key) {
        return Optional.ofNullable(params).map(p -> p.getBoolean(key)).orElse(null);
    }

    /**
     * Gets float value from params with null safety and Double to Float conversion
     * @param params the params JSONObject
     * @param key the key to lookup
     * @return the float value or null if not present
     */
    protected Float getFloatParam(JSONObject params, String key) {
        return Optional.ofNullable(params)
                .map(p -> {
                    Double d = p.getDouble(key);
                    return d != null ? d.floatValue() : null;
                })
                .orElse(null);
    }

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

    public List<BaseField> getModelParamsForm(String modelType) {
        if (modelType != null){
            if (ModelType.LLM.getKey().equals(modelType)){
                 return getChatModelParamsForm();
            }
        }
        return List.of();
    }

    protected List<BaseField> getChatModelParamsForm() {
        return new LLMChatModelParams().toForm();
    }

}
