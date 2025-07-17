package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ParaFormerSTT extends BaseSpeechToText implements BaseModel<BaseSpeechToText> {


    private String modelName;
    private ModelCredential credential;

    @Override
    public BaseSpeechToText build(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        return this;
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        StringBuilder sb = new StringBuilder();
        Transcription transcription = new Transcription();
        try {
            // 创建临时文件
            Path tempFile = Files.createTempFile("temp_audio_" + System.currentTimeMillis(), ("." + suffix));
            // 将 byte[] 写入临时文件
            Files.write(tempFile, audioBytes);
            //todo 需要上传到公网地址，通过http/https链接访问
            TranscriptionParam param=
                    TranscriptionParam.builder()
                            .apiKey(credential.getApiKey())
                            .model(modelName)
                            .fileUrls(List.of(""))
                            .parameter("language_hints", new String[]{"zh", "en"})
                            .build();
            // 提交转写请求
            TranscriptionResult result = transcription.asyncCall(param);
            // 等待转写完成
            result = transcription.wait(TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
            // 获取转写结果
            List<TranscriptionTaskResult> taskResultList = result.getResults();

            if (taskResultList != null && !taskResultList.isEmpty()) {
                for (TranscriptionTaskResult taskResult : taskResultList) {
                    String transcriptionUrl = taskResult.getTranscriptionUrl();
                    HttpURLConnection connection =
                            (HttpURLConnection) new URL(transcriptionUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    String response = HttpUtil.get(transcriptionUrl);
                    JSONObject json = JSONObject.parseObject(response);
                    String text= json.getJSONArray("transcripts").getJSONObject(0).getString("text");
                    sb.append(text);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "";
    }

}
