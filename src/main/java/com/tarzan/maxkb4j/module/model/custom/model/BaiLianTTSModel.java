package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import lombok.Data;

@Data
public class BaiLianTTSModel implements TTSModel {


    private String modelName;
    private ModelCredential credential;
    private JSONObject params;

    public BaiLianTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        this.params = params;
    }


    @Override
    public byte[] textToSpeech(String text) {
        return switch (modelName) {
            case "cosyvoice-v1", "cosyvoice-v2" -> new CosyVoiceTTS(modelName, credential, params).textToSpeech(text);
            case "sambert-v1" -> new SamBertTTS(modelName, credential, params).textToSpeech(text);
            default -> new QWenTTS(modelName, credential, params).textToSpeech(text);
        };
    }
}
