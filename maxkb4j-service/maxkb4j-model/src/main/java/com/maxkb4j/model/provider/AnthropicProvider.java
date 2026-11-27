package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.integration.credential.ModelCredentialForm;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Anthropic Claude Model Provider
 */
@Component
@ModelProviderType(provider = Provider.ANTHROPIC, name = "Anthropic", icon = Provider.ICON_ANTHROPIC)
public class AnthropicProvider extends AbsModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.ANTHROPIC;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BaseUrl.ANTHROPIC);
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AnthropicChatModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .maxTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        boolean enableThinking = getBooleanParam(params, ParamKey.ENABLE_THINKING);
        String flag = enableThinking ? Value.ENABLED : Value.DISABLED;
        params.remove(ParamKey.ENABLE_THINKING);
        params.put(ParamKey.THINKING, Map.of(ParamKey.TYPE, flag));
        return AnthropicStreamingChatModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .customParameters(params)
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }
}
