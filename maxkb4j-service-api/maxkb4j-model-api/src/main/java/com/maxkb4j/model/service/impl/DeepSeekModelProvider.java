package com.maxkb4j.model.service.impl;

import com.maxkb4j.model.custom.params.impl.LlmModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * DeepSeek Model Provider - OpenAI compatible API
 */
public class DeepSeekModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://api.deepseek.com/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("deepseek-chat", "DeepSeek Chat Model", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("deepseek-reasoner", "DeepSeek Reasoner Model", ModelType.LLM, new LlmModelParams())
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
