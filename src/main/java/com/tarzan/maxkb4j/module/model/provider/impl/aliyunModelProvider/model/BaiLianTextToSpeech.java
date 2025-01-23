package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.tarzan.maxkb4j.module.model.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToSpeech;

public class BaiLianTextToSpeech extends BaseTextToSpeech implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return null;
    }

    @Override
    public byte[] textToSpeech(String text) {
        return new byte[0];
    }
}
