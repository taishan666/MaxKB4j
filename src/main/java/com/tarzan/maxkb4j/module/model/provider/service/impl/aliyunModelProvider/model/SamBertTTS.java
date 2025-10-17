package com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseTextToSpeech;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SamBertTTS extends BaseTextToSpeech implements BaseModel<BaseTextToSpeech> {

    private SpeechSynthesisParam param;

    @Override
    public BaseTextToSpeech build(String modelName, ModelCredential modelCredential, JSONObject params) {
        String voice= params==null?"zhinan":(String) params.getOrDefault("voice","zhinan");
        int volume= params==null?50:(int) params.getOrDefault("volume",50);
        modelName = modelName.replace("sambert", ("sambert-" + voice));
        this.param = SpeechSynthesisParam.builder()
                .model(modelName)
                .apiKey(modelCredential.getApiKey())
                .text("")
                .volume(volume)
                .build();
        return this;
    }

    @Override
    public byte[] textToSpeech(String text) {
        SpeechSynthesizer synthesizer = new SpeechSynthesizer();
        param.setText(text);
        ByteBuffer audio = synthesizer.call(param);
        return audio.array();
    }
}
