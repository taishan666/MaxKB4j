package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
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
public class BaiLianTextToSpeech extends BaseTextToSpeech implements BaseModel {

    private String apiBase;
    private String apiKey;
    private String modelName;



    private static String model = "cosyvoice-v1";
    private static String voice = "longxiaochun";

    public BaiLianTextToSpeech(String apiBase, String apiKey,String modelName) {
        super();
        this.apiBase = apiBase;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) new BaiLianTextToSpeech(credential.getBaseUrl(),credential.getApiKey(),modelName);
    }

    @Override
    public byte[] textToSpeech(String text) {
        SpeechSynthesisParam param =
                SpeechSynthesisParam.builder()
                        .apiKey(this.apiKey)
                        .model(this.modelName)
                        .voice(voice)
                        .build();
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text);
        return audio.array();
    }
}
