package com.maxkb4j.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.base.entity.ModelCredential;
import com.maxkb4j.model.service.TTSModel;
import lombok.Data;

@Data
public class BaiLianTTSModel implements TTSModel {


    private String modelName;
    private ModelCredential credential;
    private JSONObject params;
    private TTSModel instance;

    public BaiLianTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        this.params = params;
        this.instance = buildInstance(modelName);
    }

    private TTSModel buildInstance(String modelName) {
        return switch (modelName) {
            case "cosyvoice-v1", "cosyvoice-v2" -> new CosyVoiceTTS(modelName, credential, params);
            case "sambert-v1" -> new SamBertTTS(modelName, credential, params);
            default -> new QWenTTS(modelName, credential, params);
        };
    }

    @Override
    public byte[] textToSpeech(String text) {
        return instance.textToSpeech(text);
    }
}
