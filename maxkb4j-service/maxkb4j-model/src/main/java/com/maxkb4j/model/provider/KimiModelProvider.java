package com.maxkb4j.model.provider;


import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * Kimi Model Provider - OpenAI compatible API
 */
public class KimiModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://api.moonshot.cn/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("kimi-k2.6", "", ModelType.LLM),
            new ModelInfo("kimi-k2.5", "", ModelType.LLM),
            new ModelInfo("kimi-k2-thinking", "", ModelType.LLM),
            new ModelInfo("kimi-k2-thinking-turbo", "", ModelType.LLM),
            new ModelInfo("moonshot-v1-8k", "", ModelType.LLM),
            new ModelInfo("moonshot-v1-32k", "", ModelType.LLM),
            new ModelInfo("moonshot-v1-128k", "", ModelType.LLM),
            new ModelInfo("kimi-k2.6", "",ModelType.VISION),
            new ModelInfo("kimi-k2.5", "", ModelType.VISION)
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
