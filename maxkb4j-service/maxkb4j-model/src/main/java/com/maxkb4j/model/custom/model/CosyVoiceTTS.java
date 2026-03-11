package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.base.entity.ModelCredential;
import com.maxkb4j.model.service.TTSModel;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class CosyVoiceTTS  implements TTSModel {

    private SpeechSynthesisParam param;

    public CosyVoiceTTS(String modelName, ModelCredential credential, JSONObject params) {
        String voice= params.getString("voice");
        Integer volume= params.getInteger("volume");
        Float speechRate=params.getFloat("speechRate");
        this.param = SpeechSynthesisParam.builder()
                        .apiKey(credential.getApiKey())
                        .model(modelName)
                        .voice(voice)
                        .speechRate(speechRate)
                        .volume(volume)
                        .build();
    }

    @Override
    public byte[] textToSpeech(String text) {
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text);
        return audio.array();
    }

}
