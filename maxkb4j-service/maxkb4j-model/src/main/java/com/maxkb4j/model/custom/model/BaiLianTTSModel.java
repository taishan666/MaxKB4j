package com.maxkb4j.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.service.ITTSModel;
import lombok.Data;
import static com.maxkb4j.model.consts.ModelConstants.*;

@Data
public class BaiLianTTSModel implements ITTSModel {


    private String modelName;
    private ModelCredential credential;
    private JSONObject params;
    private ITTSModel instance;

    public BaiLianTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        this.params = params;
        this.instance = buildInstance(modelName);
    }

    private ITTSModel buildInstance(String modelName) {
       if (modelName.startsWith(ModelName.QWEN3_TTS_PREFIX)){
           return new QWenTTS(modelName, credential, params);
       }else {
           return new BaiLianTTS(modelName, credential, params);
       }
    }

    @Override
    public byte[] textToSpeech(String text) {
        return instance.textToSpeech(text);
    }
}
