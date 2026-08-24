package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.service.ITTSModel;
import lombok.Data;

import java.nio.ByteBuffer;
import static com.maxkb4j.model.consts.ModelConstants.*;

@Data
public class BaiLianTTS implements ITTSModel {

    private SpeechSynthesisParam param;

    public BaiLianTTS(String modelName, ModelCredential credential, JSONObject params) {
        String voice = params.getString(ParamKey.VOICE);
        Integer volume = params.getInteger(ParamKey.VOLUME);
        Float speechRate = params.getFloat(ParamKey.SPEECH_RATE);
        if (ModelName.SAMBERT_V1.equals(modelName)) {
            modelName = modelName.replace(ModelName.SAMBERT_PREFIX, (ModelName.SAMBERT_NAME_PREFIX + voice));
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
