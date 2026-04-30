package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.List;

/**
 * Anthropic Claude Model Provider
 */
public class AnthropicProvider extends AbsModelProvider {

    private static final String BASE_URL = "https://api.anthropic.com";

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("claude-3-opus-20240229", "", ModelType.LLM),
            new ModelInfo("claude-3-sonnet-20240229", "", ModelType.LLM),
            new ModelInfo("claude-3-haiku-20240307", "", ModelType.LLM),
            new ModelInfo("claude-3-5-sonnet-20241022", "", ModelType.LLM),
            new ModelInfo("claude-3-5-haiku-20241022", "", ModelType.LLM),
            new ModelInfo("claude-3-5-sonnet-20241022", "", ModelType.VISION)
    );


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BASE_URL);
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AnthropicChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AnthropicStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .build();
    }
}
