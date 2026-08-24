package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Anthropic Claude Model Provider
 */
@Component
@ModelProviderType(provider = Provider.ANTHROPIC, name = "Anthropic", icon = Provider.ICON_ANTHROPIC)
public class AnthropicProvider extends AbsModelProvider {

    private static final String BASE_URL = BaseUrl.ANTHROPIC;

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.CLAUDE_3_OPUS, "", ModelType.LLM),
            new ModelInfo(ModelName.CLAUDE_3_SONNET, "", ModelType.LLM),
            new ModelInfo(ModelName.CLAUDE_3_HAIKU, "", ModelType.LLM),
            new ModelInfo(ModelName.CLAUDE_3_5_SONNET, "", ModelType.LLM),
            new ModelInfo(ModelName.CLAUDE_3_5_HAIKU, "", ModelType.LLM),
            new ModelInfo(ModelName.CLAUDE_3_5_SONNET, "", ModelType.VISION)
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
                .httpClientBuilder(getHttpClientBuilder())
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
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .customParameters(params)
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }
}
