package com.maxkb4j.model.provider;

import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * DeepSeek Model Provider - OpenAI compatible API
 */
public class DeepSeekModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://api.deepseek.com/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("deepseek-v4-flash", "", ModelType.LLM),
            new ModelInfo("deepseek-v4-pro", "", ModelType.LLM),
            new ModelInfo("deepseek-chat", "", ModelType.LLM),
            new ModelInfo("deepseek-reasoner", "", ModelType.LLM)
    );

    @Override
    public String getDefaultBaseUrl(){
        return BASE_URL;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

}
