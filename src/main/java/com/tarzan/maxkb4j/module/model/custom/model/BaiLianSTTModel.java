package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import lombok.Data;

@Data
public class BaiLianSTTModel implements STTModel {


    private String modelName;
    private ModelCredential credential;
    private JSONObject params;

    public BaiLianSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        this.params = params;
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        return switch (modelName) {
            case "gummy-realtime-v1" -> new GummySTT(modelName, credential, params).speechToText(audioBytes, suffix);
            default -> new BaiLianASR(modelName, credential, params).speechToText(audioBytes, suffix);
        };
    }
}
