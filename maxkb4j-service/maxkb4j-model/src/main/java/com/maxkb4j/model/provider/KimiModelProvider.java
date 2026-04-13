package com.maxkb4j.model.provider;


import com.maxkb4j.model.custom.params.impl.OpenAiChatModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * Kimi Model Provider - OpenAI compatible API
 */
public class KimiModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://api.moonshot.cn/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("kimi-latest", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("kimi-k2-turbo-preview", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("kimi-k2-thinking", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("kimi-k2-thinking-turbo", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("moonshot-v1-8k-vision-preview", "", ModelType.VISION, new OpenAiChatModelParams()),
            new ModelInfo("moonshot-v1-32k-vision-preview", "", ModelType.VISION, new OpenAiChatModelParams()),
            new ModelInfo("moonshot-v1-128k-vision-preview", "", ModelType.VISION, new OpenAiChatModelParams())
    );

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public String getDefaultBaseUrl(){
        return BASE_URL;
    }
}
