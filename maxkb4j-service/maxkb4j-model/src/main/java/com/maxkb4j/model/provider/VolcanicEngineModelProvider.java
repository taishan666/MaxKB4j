package com.maxkb4j.model.provider;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.entity.ModelCredential;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;


import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Volcanic Engine (Doubao) Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.VOLCANIC_ENGINE, name = "火山引擎", icon = Provider.ICON_VOLCANIC_ENGINE)
public class VolcanicEngineModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = BaseUrl.VOLCANIC_ENGINE;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.DOUBAO_1_5_PRO_32K, "", ModelType.LLM),
            new ModelInfo(ModelName.DOUBAO_SEED_1_6, "", ModelType.LLM),
            new ModelInfo(ModelName.DOUBAO_SEED_1_6_FLASH, "", ModelType.LLM),
            new ModelInfo(ModelName.DOUBAO_SEED_1_6_THINKING, "", ModelType.LLM),
            new ModelInfo(ModelName.DOUBAO_SEED_1_6_VISION, "", ModelType.VISION),
            new ModelInfo(ModelName.DOUBAO_SEEDREAM_4_0, "", ModelType.TTI),
            new ModelInfo(ModelName.DOUBAO_SEEDREAM_4_5, "", ModelType.TTI),
            new ModelInfo(ModelName.DOUBAO_EMBEDDING_TEXT, "", ModelType.EMBEDDING)
    );

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


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public String getDefaultBaseUrl(){
        return BASE_URL;
    }
}
