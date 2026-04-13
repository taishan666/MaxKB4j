package com.maxkb4j.model.provider;


import com.maxkb4j.model.custom.params.impl.OpenAiChatModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * XunFei (iFLYTEK) Model Provider - OpenAI compatible API
 */
public class XunFeiModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://spark-api-open.xf-yun.com/v1/";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("4.0Ultra", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("max-32k", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("generalv3.5", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("generalv3", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("lite", "", ModelType.LLM, new OpenAiChatModelParams())
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
