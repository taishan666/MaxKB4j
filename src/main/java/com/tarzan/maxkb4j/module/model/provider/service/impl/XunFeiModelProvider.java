package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;

import java.util.List;

/**
 * XunFei (iFLYTEK) Model Provider
 * Note: This provider currently only returns model info without implementation
 */
public class XunFeiModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://spark-api-open.xf-yun.com/v1/";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("4.0Ultra", "", ModelType.LLM),
            new ModelInfo("max-32k", "", ModelType.LLM),
            new ModelInfo("generalv3.5", "", ModelType.LLM),
            new ModelInfo("generalv3", "", ModelType.LLM),
            new ModelInfo("lite", "", ModelType.LLM)
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
