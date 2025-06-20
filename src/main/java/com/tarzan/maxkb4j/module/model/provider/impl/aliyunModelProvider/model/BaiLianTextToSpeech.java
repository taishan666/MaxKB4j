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
public class BaiLianTextToSpeech extends BaseTextToSpeech implements BaseModel<BaseTextToSpeech> {

    private SpeechSynthesisParam param;
    private static String defaultVoice = "longxiaochun";


    public BaiLianTextToSpeech(SpeechSynthesisParam param) {
        super();
        this.param = param;
    }
    @Override
    public BaiLianTextToSpeech build(String modelName, ModelCredential credential, JSONObject params) {
        SpeechSynthesisParam param =
                SpeechSynthesisParam.builder()
                        .apiKey(credential.getApiKey())
                        .model(modelName)
                        .voice(params==null?defaultVoice:params.getString("voice"))
                        .speechRate(params==null?1.0f:params.getFloat("speechRate"))
                        .volume(params==null?50:params.getInteger("volume"))
                        .build();
        return  new BaiLianTextToSpeech(param);
    }

    @Override
    public byte[] textToSpeech(String text) {
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text);
        return audio.array();
    }
}
