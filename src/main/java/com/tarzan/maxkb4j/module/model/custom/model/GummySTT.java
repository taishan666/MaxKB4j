package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerRealtime;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


@Data
public class GummySTT implements STTModel {

    private TranslationRecognizerParam param;
    private String[] translationLanguages;

    public GummySTT(String modelName, ModelCredential modelCredential, JSONObject params) {
        if (params != null){
            String targetLanguage =params.getString("targetLanguage");
            if (targetLanguage != null){
                this.translationLanguages = new String[]{targetLanguage};
            }
        }else {
            this.translationLanguages = new String[]{};
        }
        this.param = TranslationRecognizerParam.builder()
                         .apiKey(modelCredential.getApiKey())
                        .model(modelName)
                        .format("wav") // 'pcm'、'wav'、'mp3'、'opus'、'speex'、'aac'、'amr', you
                        .sampleRate(16000)
                        .transcriptionEnabled(true)
                        .sourceLanguage("auto")
                        .translationEnabled(true)
                        .translationLanguages(translationLanguages)
                        .build();
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        TranslationRecognizerRealtime translator = new TranslationRecognizerRealtime();
        CountDownLatch latch = new CountDownLatch(1);
        List<String> texts = new ArrayList<>();
        ResultCallback<TranslationRecognizerResult> callback = new ResultCallback<>() {
            @Override
            public void onEvent(TranslationRecognizerResult result) {
                if (result.isSentenceEnd()) {
                    texts.add(result.getTranscriptionResult().getText());
                }
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                System.out.println("RecognitionCallback error: " + e.getMessage());
            }
        };
        // 将录音音频数据发送给流式识别服务
        translator.call(param, callback);
        try {
            InputStream fis = new ByteArrayInputStream(audioBytes);
            // chunk size set to 1 seconds for 16KHz sample rate
            byte[] buffer = new byte[3200];
            int bytesRead;
            // Loop to read chunks of the file
            while ((bytesRead = fis.read(buffer)) != -1) {
                ByteBuffer byteBuffer;
                // Handle the last chunk which might be smaller than the buffer size
                if (bytesRead < buffer.length) {
                    byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                } else {
                    byteBuffer = ByteBuffer.wrap(buffer);
                }
                translator.sendAudioFrame(byteBuffer);
                buffer = new byte[3200];
                Thread.sleep(50);
            }
            translator.stop();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }finally {
            // 任务结束关闭 websocket 连接
            translator.getDuplexApi().close(1000, "bye");
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return String.join("", texts);
    }
}
