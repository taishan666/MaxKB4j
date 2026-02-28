package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.custom.params.impl.ImageModelParams;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.custom.params.impl.TextEmbeddingV3Params;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;

import java.util.List;

/**
 * Volcanic Engine (Doubao) Model Provider - OpenAI compatible API
 */
public class VolcanicEngineModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("doubao-1-5-pro-32k-250115", "大语言模型", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("doubao-seed-1-6-251015", "大语言模型", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("doubao-seed-1-6-flash-250828", "大语言模型", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("doubao-seed-1-6-thinking-250715", "文本向量模型", ModelType.LLM),
            new ModelInfo("doubao-seed-1-6-vision-250815", "图片理解", ModelType.VISION, new LlmModelParams()),
            new ModelInfo("doubao-seedream-4-0-250828", "图片生成", ModelType.TTI, new ImageModelParams()),
            new ModelInfo("doubao-seedream-4-5-251128", "图片生成", ModelType.TTI, new ImageModelParams()),
            new ModelInfo("doubao-embedding-text-240715", "文本向量模型", ModelType.EMBEDDING, new TextEmbeddingV3Params())
    );


    @Override
    public ModelProviderInfo getBaseInfo() {
        return new ModelProviderInfo(ModelProviderEnum.VolcanicEngine);
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BASE_URL);
    }
}
