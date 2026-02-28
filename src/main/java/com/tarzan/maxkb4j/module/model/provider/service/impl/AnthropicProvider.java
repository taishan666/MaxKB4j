package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.List;

public class AnthropicProvider extends IModelProvider {

    private final HttpClientBuilder httpClientBuilder = buildHttpClientBuilder();

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("claude-3-opus-20240229", "大语言模型", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("claude-3-sonnet-20240229", "大语言模型", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("claude-3-haiku-20240307", "大语言模型", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("claude-3-5-sonnet-20241022", "大语言模型", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("claude-3-5-haiku-20241022", "大语言模型", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("claude-3-5-sonnet-20241022", "AI视觉模型", ModelType.VISION, new LlmModelParams())
    );

    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.Anthropic);
        info.setIcon(getSvgIcon("anthropic_icon.svg"));
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }


    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AnthropicChatModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AnthropicStreamingChatModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

}
