package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.ParamKey;
import static com.maxkb4j.model.consts.ModelConstants.Provider;

/**
 * Google Gemini Model Provider
 */
@Component
@ModelProviderType(provider = Provider.GEMINI, name = "Google Gemini", icon = Provider.ICON_GEMINI)
public class GeminiModelProvider extends AbsModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.GEMINI;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        boolean enableThinking = getBooleanParam(params, ParamKey.ENABLE_THINKING);
        return GoogleAiGeminiChatModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .thinkingConfig(GeminiThinkingConfig.builder().includeThoughts(enableThinking).build())
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        boolean enableThinking = getBooleanParam(params, ParamKey.ENABLE_THINKING);
        return GoogleAiGeminiStreamingChatModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .thinkingConfig(GeminiThinkingConfig.builder().includeThoughts(enableThinking).build())
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return GoogleAiEmbeddingModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
