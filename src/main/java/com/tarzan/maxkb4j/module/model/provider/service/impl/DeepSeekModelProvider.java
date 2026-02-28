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
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.List;

public class DeepSeekModelProvider extends IModelProvider {

    private final String BASE_URL = "https://api.deepseek.com/v1";

    private final HttpClientBuilder httpClientBuilder = buildHttpClientBuilder();

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("deepseek-chat", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("deepseek-reasoner", "", ModelType.LLM, new LlmModelParams())
    );


    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.DeepSeek);
        info.setIcon(getSvgIcon("deepseek_icon.svg"));
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiChatModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .baseUrl(BASE_URL)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .baseUrl(BASE_URL)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

}
