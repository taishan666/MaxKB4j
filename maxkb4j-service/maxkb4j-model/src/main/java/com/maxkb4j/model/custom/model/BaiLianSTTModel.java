package com.maxkb4j.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.base.STTModel;
import lombok.Data;

import static com.maxkb4j.model.consts.ModelConstants.*;

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
        if (modelName.startsWith(ModelName.GUMMY_PREFIX)) {
            return new GummySTT(modelName, credential, params);
        }
        return new BaiLianASRRealtime(modelName, credential, params);
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        return instance.speechToText(audioBytes, suffix);
    }
}
