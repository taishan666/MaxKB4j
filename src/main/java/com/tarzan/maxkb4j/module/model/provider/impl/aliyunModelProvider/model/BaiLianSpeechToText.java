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
public class BaiLianSpeechToText extends BaseSpeechToText implements BaseModel {

    private String apiBase;
    private String apiKey;


    public BaiLianSpeechToText(String apiBase, String apiKey) {
        super();
        this.apiBase = apiBase;
        this.apiKey = apiKey;
    }

    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) new BaiLianSpeechToText(credential.getBaseUrl(), credential.getApiKey());
    }

    @Override
    public String speechToText(byte[] audioBytes) {
        // 创建Recognition实例
        Recognition recognizer = new Recognition();
        // 创建RecognitionParam
        RecognitionParam param =
                RecognitionParam.builder()
                        .apiKey(apiKey)
                        .model("paraformer-realtime-v2")
                        .format("mp3")
                        .sampleRate(22050)
                        .parameter("language_hints", new String[]{"zh", "en"})
                        .build();
        // 创建临时文件
        Path tempFile;
        try {
            tempFile = Files.createTempFile("temp_audio",".mp3");
            // 将 byte[] 写入临时文件
            Files.write(tempFile, audioBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String result= recognizer.call(param, tempFile.toFile());
        JSONObject json = JSON.parseObject(result);
        JSONArray sentences= json.getJSONArray("sentences");
        return sentences.getJSONObject(0).getString("text");
    }

}
