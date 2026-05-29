package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.custom.params.OLlamaChatModelParams;
import com.maxkb4j.model.custom.params.OllamaImageModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaImageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Ollama Model Provider - Local deployment
 */
public class OLlamaModelProvider extends AbsModelProvider {

    private static final String BASE_URL = "http://host.docker.internal:11434";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("qwen:7b", "", ModelType.LLM),
            new ModelInfo("llama3:8b", "", ModelType.LLM),
            new ModelInfo("deepseek-r1:8b", "", ModelType.LLM),
            new ModelInfo("nomic-embed-text", "", ModelType.EMBEDDING),
            new ModelInfo("llava:7b", "", ModelType.VISION),
            new ModelInfo("llava:13b", "", ModelType.VISION),
            new ModelInfo("x/z-image-turbo", "", ModelType.TTI)
    );

    /**
     * 重写父类方法，为 Ollama 提供 UTF-8 编码支持的 HTTP 客户端
     */
    @Override
    protected HttpClientBuilder getHttpClientBuilder() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE + "; charset=UTF-8")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return SpringRestClient.builder()
                .restClientBuilder(restClientBuilder);
    }

    @Override
    public List<BaseField> getChatModelParamsForm() {
        return new OLlamaChatModelParams().toForm();
    }

    @Override
    protected List<BaseField> getImageModelParamsForm() {
        return new OllamaImageModelParams().toForm();
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(false, BASE_URL);
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .think(getBooleanParam(params,"enableThinking"))
                .returnThinking(true)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaStreamingChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .think(getBooleanParam(params,"enableThinking"))
                .returnThinking(true)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaEmbeddingModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return  OllamaImageModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .width(1024)
                .height(768)
                .steps(params.getInteger("steps"))
                .seed(params.getInteger("seed"))
                .build();
    }
}
