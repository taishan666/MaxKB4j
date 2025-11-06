package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@Data
@NoArgsConstructor
public class SamBertTTS  implements TTSModel {

    private SpeechSynthesisParam param;

    public SamBertTTS(String modelName, ModelCredential modelCredential, JSONObject params) {
        String voice= params.getString("voice");
        Integer volume= params.getInteger("volume");
        modelName = modelName.replace("sambert", ("sambert-" + voice));
        this.param = SpeechSynthesisParam.builder()
                .model(modelName)
                .apiKey(modelCredential.getApiKey())
                .text("")
                .volume(volume)
                .build();
    }

    @Override
    public byte[] textToSpeech(String text) {
        SpeechSynthesizer synthesizer = new SpeechSynthesizer();
        param.setText(text);
        ByteBuffer audio = synthesizer.call(param);
        return audio.array();
    }
}
