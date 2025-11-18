package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import lombok.Data;

@Data
public class QwenASR implements STTModel {


    public QwenASR(String modelName, ModelCredential credential, JSONObject params) {

    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {

        return "";
    }
}
