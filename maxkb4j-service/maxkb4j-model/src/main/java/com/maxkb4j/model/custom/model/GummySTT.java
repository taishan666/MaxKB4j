package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerRealtime;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;


@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class GummySTT extends AbsSTTModel {

    private TranslationRecognizerParam param;
    private String translationLanguage;

    public GummySTT(String modelName, ModelCredential modelCredential, JSONObject params) {
        if (params != null){
            String  targetLanguage=params.getString("targetLanguage");
            if (targetLanguage != null){
                this.translationLanguage = targetLanguage;
            }
        }else {
            this.translationLanguage = "en";
        }
        this.param = TranslationRecognizerParam.builder()
                         .apiKey(modelCredential.getApiKey())
                        .model(modelName)
                        .format("mp3") // 'pcm'、'wav'、'mp3'、'opus'、'speex'、'aac'、'amr', you
                        .sampleRate(16000)
                        .transcriptionEnabled(true)
                        .sourceLanguage("auto")
                        .translationEnabled(false)
                        .translationLanguages(new String[]{this.translationLanguage})
                        .build();
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        TranslationRecognizerRealtime translator = new TranslationRecognizerRealtime();
        AtomicReference<String> resultText = new AtomicReference<>("");
        ResultCallback<TranslationRecognizerResult> callback = new ResultCallback<>() {
            @Override
            public void onEvent(TranslationRecognizerResult result) {
                if (result.isSentenceEnd()) {
                    resultText.set(result.getTranscriptionResult().getText());
                }
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Exception e) {
                log.error("RecognitionCallback error: {}", e.getMessage());
            }
        };
        int sampleRate=getSampleRate(audioBytes, "."+suffix);
        this.param.setSampleRate(sampleRate);
        String format = suffix != null ? suffix.toLowerCase() : "mp3";
        this.param.setFormat(format);
        // 将录音音频数据发送给流式识别服务
        translator.call(param, callback);
        int sendFrameLength = 3200;
        for (int i = 0; i * sendFrameLength < audioBytes.length; i ++) {
            int start = i * sendFrameLength;
            int end = Math.min(start + sendFrameLength, audioBytes.length);
            ByteBuffer byteBuffer = ByteBuffer.wrap(audioBytes, start, end - start);
            translator.sendAudioFrame(byteBuffer);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        translator.stop();
        return resultText.get();
    }
}
