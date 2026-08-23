package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Google Gemini Model Provider
 */
@Component
@ModelProviderType(provider = Provider.GEMINI, name = "Google Gemini", icon = Provider.ICON_GEMINI)
public class GeminiModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.GEMINI_1_0_PRO, "", ModelType.LLM),
            new ModelInfo(ModelName.GEMINI_1_0_PRO_VISIO, "", ModelType.LLM),
            new ModelInfo(ModelName.GEMINI_EMBEDDING_001, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.GEMINI_1_5_FLASH, "", ModelType.VISION),
            new ModelInfo(ModelName.GEMINI_1_5_PRO, "", ModelType.VISION)
    );


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        boolean enableThinking = getBooleanParam(params, ParamKey.ENABLE_THINKING);
        return GoogleAiGeminiChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
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
                .httpClientBuilder(getHttpClientBuilder())
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
                .httpClientBuilder(getHttpClientBuilder())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
