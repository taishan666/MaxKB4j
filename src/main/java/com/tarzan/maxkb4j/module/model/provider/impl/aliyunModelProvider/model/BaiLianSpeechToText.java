package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;

import java.io.File;

public class BaiLianSpeechToText extends BaseSpeechToText implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, JSONObject modelCredential) {
        return null;
    }

    @Override
    public String speechToText(File audioFile) {
        return "";
    }
}
