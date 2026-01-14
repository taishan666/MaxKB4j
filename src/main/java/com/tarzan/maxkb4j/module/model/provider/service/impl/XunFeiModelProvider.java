package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;

import java.util.ArrayList;
import java.util.List;

public class XunFeiModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.XunFei);
        info.setIcon(getSvgIcon("xf_icon.svg"));
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("generalv3.5","", ModelType.LLM));
        modelInfos.add(new ModelInfo("generalv3","",ModelType.LLM));
        modelInfos.add(new ModelInfo("generalv2","",ModelType.LLM));
        modelInfos.add(new ModelInfo("embedding","",ModelType.EMBEDDING));
        return modelInfos;
    }


}
