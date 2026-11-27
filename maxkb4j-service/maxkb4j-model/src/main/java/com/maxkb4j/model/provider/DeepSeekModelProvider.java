package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * DeepSeek Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.DEEP_SEEK, name = "DeepSeek", icon = Provider.ICON_DEEP_SEEK)
public class DeepSeekModelProvider extends OpenAiModelProvider {

    @Override
    public String getDefaultBaseUrl() {
        return BaseUrl.DEEP_SEEK;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.DEEP_SEEK;
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        boolean enableThinking = getBooleanParam(params, ParamKey.ENABLE_THINKING);
        String flag = enableThinking ? Value.ENABLED : Value.DISABLED;
        params.remove(ParamKey.ENABLE_THINKING);
        params.put(ParamKey.THINKING, Map.of(ParamKey.TYPE, flag));
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(getJdkHttpClientBuilder())
                .baseUrl(getBaseUrl(credential.getBaseUrl()))
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .customParameters(params)
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }

}
