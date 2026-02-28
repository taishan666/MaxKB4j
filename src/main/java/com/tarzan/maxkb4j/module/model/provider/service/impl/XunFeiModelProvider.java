package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.AbsModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;

import java.util.List;

/**
 * XunFei (iFLYTEK) Model Provider
 * Note: This provider currently only returns model info without implementation
 */
public class XunFeiModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("generalv3.5", "", ModelType.LLM),
            new ModelInfo("generalv3", "", ModelType.LLM),
            new ModelInfo("generalv2", "", ModelType.LLM),
            new ModelInfo("embedding", "", ModelType.EMBEDDING)
    );



    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }
}
