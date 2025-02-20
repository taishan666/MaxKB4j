package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.tarzan.maxkb4j.module.model.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;

public class QWenTextToImageModel implements BaseModel {

    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) WanxImageModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName).build();
    }




}
