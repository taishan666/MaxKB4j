package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.service.STTModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class BaiLianASRRealtime implements STTModel {


    private RecognitionParam param;
    private String modelName;
    private ModelCredential credential;

    private static final List<String> SUPPORT_MODELS = List.of("fun-asr-realtime", "paraformer-realtime-v2", "gummy-realtime-v1");

    public BaiLianASRRealtime(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        this.param = buildParam("wav", 16000);
    }

    private RecognitionParam buildParam(String format, int sampleRate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("language_hints", new String[]{"zh", "en"});
        if ("fun-asr-realtime".equals(modelName)) {
            parameters.put("disfluency_removal_enabled", false);
            parameters.put("show_punctuation", true);
            parameters.put("inverse_text_normalization", true);
        } else if ("paraformer-realtime-v2".equals(modelName)) {
            parameters.put("disfluency_removal_enabled", false);
        }
        return RecognitionParam.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .format(format.toLowerCase())
                .sampleRate(sampleRate)
                .parameters(parameters)
                .build();
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        log.info("========== 语音识别开始 ==========");
        log.info("使用模型: {}", modelName);
        log.info("音频数据大小: {} bytes", audioBytes.length);
        log.info("文件后缀: {}", suffix);

        String format = suffix != null ? suffix.toLowerCase() : "wav";
        int sampleRate = 22050;
        this.param = buildParam(format, sampleRate);
        log.info("使用格式: {}, 采样率: {}", format, sampleRate);

        InputStream in = new ByteArrayInputStream(audioBytes);
        String fileName = "asr_example." + format;
        try {
            Files.copy(in, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
            File savedFile = new File(fileName);
            log.info("文件已保存: {}", savedFile.getAbsolutePath());
            log.info("文件大小: {} bytes", savedFile.length());
        } catch (IOException e) {
            System.err.println("文件保存失败: " + e.getMessage());
            throw new RuntimeException("文件保存失败", e);
        }

        Recognition recognizer = new Recognition();
        String result = recognizer.call(param, new File(fileName));

        JSONObject json = JSONObject.parseObject(result);

        if (json.containsKey("code")) {
            log.error("ASR调用错误码: {}", json.getString("code"));
            log.error("ASR错误消息: {}", json.getString("message"));
            return "";
        }

        List<String> texts = new ArrayList<>();
        JSONArray sentences = json.getJSONArray("sentences");

        if (sentences == null || sentences.isEmpty()) {
            return "";
        }

        for (int i = 0; i < sentences.size(); i++) {
            JSONObject sentence = sentences.getJSONObject(i);
            String text = sentence.getString("text");
            if (text != null && !text.trim().isEmpty() && sentence.getBooleanValue("sentence_end")) {
                texts.add(text);
            }
        }

        String finalResult = String.join("", texts);
        log.info("最终识别文本: [{}]", finalResult);
        log.info("========== 语音识别结束 ==========");
        return finalResult;
    }

}
