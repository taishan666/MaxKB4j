package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek Model Provider - OpenAI compatible API
 */
public class DeepSeekModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://api.deepseek.com/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("deepseek-v4-flash", "", ModelType.LLM),
            new ModelInfo("deepseek-v4-pro", "", ModelType.LLM),
            new ModelInfo("deepseek-chat", "", ModelType.LLM),
            new ModelInfo("deepseek-reasoner", "", ModelType.LLM)
    );

    @Override
    public String getDefaultBaseUrl(){
        return BASE_URL;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        boolean enableThinking = getBooleanParam(params, "enableThinking");
        String flag = enableThinking ? "enabled" : "disabled";
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(getBaseUrl(credential.getBaseUrl()))
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .customParameters(Map.of("thinking", Map.of("type", flag)))
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }

}
