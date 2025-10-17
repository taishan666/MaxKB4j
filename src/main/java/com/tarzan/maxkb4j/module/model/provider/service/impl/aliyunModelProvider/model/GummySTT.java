package com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerRealtime;
import com.alibaba.dashscope.audio.asr.translation.results.TranscriptionResult;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResultPack;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseSpeechToText;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class GummySTT extends BaseSpeechToText implements BaseModel<BaseSpeechToText> {

    private TranslationRecognizerParam param;
    private String[] translationLanguages;
    @Override
    public BaseSpeechToText build(String modelName, ModelCredential modelCredential, JSONObject params) {
        if (params != null){
            String targetLanguage =params.getString("targetLanguage");
            if (targetLanguage != null){
                this.translationLanguages = new String[]{targetLanguage};
            }
        }else {
            this.translationLanguages = new String[]{};
        }
        this.param =
                TranslationRecognizerParam.builder()
                         .apiKey(modelCredential.getApiKey())
                        .model(modelName)
                        .format("wav") // 'pcm'、'wav'、'mp3'、'opus'、'speex'、'aac'、'amr', you
                        .sampleRate(16000)
                        .transcriptionEnabled(true)
                        .sourceLanguage("auto")
                        .translationEnabled(true)
                        .translationLanguages(translationLanguages)
                        .build();
        return this;
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        StringBuilder sb = new StringBuilder();
        TranslationRecognizerRealtime translator = new TranslationRecognizerRealtime();
        // 创建临时文件
        Path tempFile;
        try {
            tempFile = Files.createTempFile("temp_audio_"+System.currentTimeMillis(),"."+suffix);
            // 将 byte[] 写入临时文件
            Files.write(tempFile, audioBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TranslationRecognizerResultPack result = translator.call(param, tempFile.toFile());
        if (result.getError() != null) {
            System.out.println("error: " + result.getError());
            throw new RuntimeException(result.getError());
        } else {
            ArrayList<TranscriptionResult> transcriptionResults  = result.getTranscriptionResultList();
            for (TranscriptionResult transcriptionResult : transcriptionResults) {
                sb.append(transcriptionResult.getText());
            }
        }
        return sb.toString();
    }
}
