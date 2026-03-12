package com.maxkb4j.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.service.STTModel;
import lombok.Data;

@Data
public class BaiLianSTTModel implements STTModel {


    private String modelName;
    private ModelCredential credential;
    private JSONObject params;
    private STTModel instance;

    public BaiLianSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        this.params = params;
        this.instance = buildInstance(modelName);
    }

    private STTModel buildInstance(String modelName) {
        return switch (modelName) {
            case "gummy-realtime-v1" -> new GummySTT(modelName, credential, params);
            default -> new BaiLianASR(modelName, credential, params);
        };
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        return instance.speechToText(audioBytes, suffix);
    }
}
