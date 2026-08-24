package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * DeepSeek Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.DEEP_SEEK, name = "DeepSeek", icon = Provider.ICON_DEEP_SEEK)
public class DeepSeekModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = BaseUrl.DEEP_SEEK;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.DEEPSEEK_V4_FLASH, "", ModelType.LLM),
            new ModelInfo(ModelName.DEEPSEEK_V4_PRO, "", ModelType.LLM),
            new ModelInfo(ModelName.DEEPSEEK_CHAT, "", ModelType.LLM),
            new ModelInfo(ModelName.DEEPSEEK_REASONER, "", ModelType.LLM)
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
        boolean enableThinking = getBooleanParam(params, ParamKey.ENABLE_THINKING);
        String flag = enableThinking ? Value.ENABLED : Value.DISABLED;
        params.remove(ParamKey.ENABLE_THINKING);
        params.put(ParamKey.THINKING, Map.of(ParamKey.TYPE, flag));
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(getBaseUrl(credential.getBaseUrl()))
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .customParameters(params)
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }

}
