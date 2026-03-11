package com.maxkb4j.model.provider;


import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * Tencent HunYuan Model Provider - OpenAI compatible API
 */
public class TencentModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://api.hunyuan.cloud.tencent.com/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("hunyuan-pro", "", ModelType.LLM),
            new ModelInfo("hunyuan-standard", "", ModelType.LLM),
            new ModelInfo("hunyuan-lite", "", ModelType.LLM),
            new ModelInfo("hunyuan-role", "", ModelType.LLM),
            new ModelInfo("hunyuan-functioncall", "", ModelType.LLM),
            new ModelInfo("hunyuan-code", "", ModelType.LLM),
            new ModelInfo("hunyuan-embedding", "", ModelType.EMBEDDING),
            new ModelInfo("hunyuan-vision", "", ModelType.VISION),
            new ModelInfo("hunyuan-dit", "", ModelType.TTI)
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
