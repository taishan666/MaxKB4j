package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ParaFormerSTT extends BaseSpeechToText implements BaseModel<BaseSpeechToText> {

    private RecognitionParam param;

    @Override
    public BaseSpeechToText build(String modelName, ModelCredential credential, JSONObject params) {
        this.param =
                RecognitionParam.builder()
                        .apiKey(credential.getApiKey())
                        .model(modelName)
                        .format("mp3")
                        .sampleRate(16000)
                        .parameter("language_hints", new String[]{"zh", "en"})
                        .build();
        return  this;
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        // 创建Recognition实例
        Recognition recognizer = new Recognition();
        // 创建临时文件
        Path tempFile;
        try {
            tempFile = Files.createTempFile("temp_audio_"+System.currentTimeMillis(),"."+suffix);
            // 将 byte[] 写入临时文件
            Files.write(tempFile, audioBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String result= recognizer.call(param, tempFile.toFile());
        JSONObject json = JSON.parseObject(result);
        JSONArray sentences= json.getJSONArray("sentences");
        if (sentences==null||sentences.isEmpty()){
            return "";
        }
        return sentences.getJSONObject(0).getString("text");
    }

}
