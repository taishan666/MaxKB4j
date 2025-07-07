package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.audio.ttsv2.enrollment.Voice;
import com.alibaba.dashscope.audio.ttsv2.enrollment.VoiceEnrollmentService;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
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
    private final String defaultVoice="longxiaochun";

    @Override
    public CosyVoiceTTS build(String modelName, ModelCredential credential, JSONObject params) {
        String voice= params==null?defaultVoice:(String) params.getOrDefault("voice",defaultVoice);
        int volume= params==null?50:(int) params.getOrDefault("volume",50);
        float speechRate=params==null?1F:(int) params.getFloatValue("speechRate");
        this.param = SpeechSynthesisParam.builder()
                        .apiKey(credential.getApiKey())
                        .model(modelName)
                        .voice(voice)
                        .speechRate(speechRate)
                        .volume(volume)
                        .build();
        return  this;
    }

    @Override
    public byte[] textToSpeech(String text) {
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text);
        return audio.array();
    }

    public String voiceClone(String fileUrl) {
        VoiceEnrollmentService service = new VoiceEnrollmentService(param.getApiKey());
        Voice myVoice;
        try {
            myVoice = service.createVoice(param.getModel(), "clone_voice", fileUrl);
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new RuntimeException(e);
        }
        System.out.println("your voice id is " + myVoice.getVoiceId());
        System.out.println("voice status is " + myVoice.getStatus());
        return myVoice.getVoiceId();
    }
}
