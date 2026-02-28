package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;

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
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BASE_URL);
    }

}
