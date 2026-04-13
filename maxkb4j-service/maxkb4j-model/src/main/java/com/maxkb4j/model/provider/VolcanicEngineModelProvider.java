package com.maxkb4j.model.provider;


import com.maxkb4j.model.custom.params.impl.ImageModelParams;
import com.maxkb4j.model.custom.params.impl.OpenAiChatModelParams;
import com.maxkb4j.model.custom.params.impl.TextEmbeddingV3Params;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * Volcanic Engine (Doubao) Model Provider - OpenAI compatible API
 */
public class VolcanicEngineModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("doubao-1-5-pro-32k-250115", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("doubao-seed-1-6-251015", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("doubao-seed-1-6-flash-250828", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("doubao-seed-1-6-thinking-250715", "", ModelType.LLM, new OpenAiChatModelParams()),
            new ModelInfo("doubao-seed-1-6-vision-250815", "", ModelType.VISION, new OpenAiChatModelParams()),
            new ModelInfo("doubao-seedream-4-0-250828", "", ModelType.TTI, new ImageModelParams()),
            new ModelInfo("doubao-seedream-4-5-251128", "", ModelType.TTI, new ImageModelParams()),
            new ModelInfo("doubao-embedding-text-240715", "", ModelType.EMBEDDING, new TextEmbeddingV3Params())
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
