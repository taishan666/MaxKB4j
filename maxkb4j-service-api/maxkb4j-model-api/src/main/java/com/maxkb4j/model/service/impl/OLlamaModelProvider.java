package com.maxkb4j.model.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.base.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.custom.params.impl.LlmModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.AbsModelProvider;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

import java.util.List;

/**
 * Ollama Model Provider - Local deployment
 */
public class OLlamaModelProvider extends AbsModelProvider {

    private static final String BASE_URL = "http://host.docker.internal:11434";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("qwen:7b", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("llama3:8b", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("deepseek-r1:8b", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("nomic-embed-text", "", ModelType.EMBEDDING),
            new ModelInfo("llava:7b", "", ModelType.VISION, new LlmModelParams()),
            new ModelInfo("llava:13b", "", ModelType.VISION, new LlmModelParams())
    );

    private final HttpClientBuilder jdkHttpClientBuilder= new JdkHttpClientBuilder();

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
                .httpClientBuilder(jdkHttpClientBuilder)
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .returnThinking(true)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaStreamingChatModel.builder()
                .httpClientBuilder(jdkHttpClientBuilder)
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
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
}
