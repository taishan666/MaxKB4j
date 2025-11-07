package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Data
public class FunASR implements STTModel {

    private RecognitionParam param;

    public FunASR(String modelName, ModelCredential credential, JSONObject params) {
        this.param = RecognitionParam.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .format("wav")
                .sampleRate(16000)
                .parameter("language_hints", new String[]{"zh", "en"})
                .build();
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        InputStream in = new ByteArrayInputStream(audioBytes);
        try {
            Files.copy(in, Paths.get("asr_example.wav"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Recognition recognizer = new Recognition();
        String result = recognizer.call(param, new File("asr_example.wav"));
        JSONObject json = JSONObject.parseObject(result);
        List<String> texts = new ArrayList<>();
        JSONArray sentences=json.getJSONArray("sentences");
        for (int i = 0; i < sentences.size(); i++) {
            JSONObject sentence = sentences.getJSONObject(i);
            texts.add(sentence.getString("text"));
        }
        return String.join("", texts);
    }
}
