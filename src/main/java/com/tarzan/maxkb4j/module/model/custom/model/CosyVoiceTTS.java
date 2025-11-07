package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
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

/*    public String voiceClone(String fileUrl) {
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
    }*/
}
