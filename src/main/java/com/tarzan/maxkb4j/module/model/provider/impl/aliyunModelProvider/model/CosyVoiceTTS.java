package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToSpeech;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CosyVoiceTTS extends BaseTextToSpeech implements BaseModel<BaseTextToSpeech> {

    private SpeechSynthesisParam param;

    @Override
    public CosyVoiceTTS build(String modelName, ModelCredential credential, JSONObject params) {
        String voice= (String) params.getOrDefault("voice","longxiaochun");
        this.param =
                SpeechSynthesisParam.builder()
                        .apiKey(credential.getApiKey())
                        .model(modelName)
                        .voice(voice)
                        .speechRate(params.getFloat("speechRate"))
                        .volume(params.getInteger("volume")==null?50:params.getInteger("volume"))
                        .build();
        return  this;
    }

    @Override
    public byte[] textToSpeech(String text) {
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text);
        return audio.array();
    }
}
