package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.service.TTSModel;
import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class BaiLianTTS implements TTSModel {

    private SpeechSynthesisParam param;

    public BaiLianTTS(String modelName, ModelCredential credential, JSONObject params) {
        String voice = params.getString("voice");
        Integer volume = params.getInteger("volume");
        Float speechRate = params.getFloat("speechRate");
        if ("sambert-v1".equals(modelName)) {
            modelName = modelName.replace("sambert", ("sambert-" + voice));
        }
        this.param = SpeechSynthesisParam.builder()
                .model(modelName)
                .apiKey(credential.getApiKey())
                .speechRate(speechRate == null ? 1.0f : speechRate)
                .volume(volume == null ? 50 : volume)
                .voice(voice)
                .build();
    }

    @Override
    public byte[] textToSpeech(String text) {
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text);
        return audio.array();
    }

}
