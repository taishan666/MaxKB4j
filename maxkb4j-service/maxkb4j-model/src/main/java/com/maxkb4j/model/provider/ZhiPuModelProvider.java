package com.maxkb4j.model.provider;

import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * ZhiPu (GLM) Model Provider
 */
public class ZhiPuModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://open.bigmodel.cn/api/paas/v4";

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("glm-5.1", "", ModelType.LLM),
            new ModelInfo("glm-5", "", ModelType.LLM),
            new ModelInfo("glm-4", "", ModelType.LLM),
            new ModelInfo("glm-4v", "", ModelType.LLM),
            new ModelInfo("glm-3-turbo", "", ModelType.LLM),
            new ModelInfo("embedding-3", "", ModelType.EMBEDDING),
            new ModelInfo("glm-4v-plus", "", ModelType.VISION),
            new ModelInfo("glm-4v", "", ModelType.VISION),
            new ModelInfo("glm-4v-flash", "", ModelType.VISION),
            new ModelInfo("glm-image", "", ModelType.TTI),
            new ModelInfo("cogview-4", "", ModelType.TTI),
            new ModelInfo("cogview-3-flash", "", ModelType.TTI)
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
