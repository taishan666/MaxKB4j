package com.tarzan.maxkb4j.module.model.custom.model;

import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SenseVoiceSTT implements STTModel {

    private String modelName;
    private ModelCredential credential;

    public SenseVoiceSTT(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        StringBuilder sb = new StringBuilder();
        try {
            Transcription transcription = new Transcription();
            // 创建临时文件
            Path tempFile= Files.createTempFile("temp_audio_"+System.currentTimeMillis(),("."+suffix));
            // 将 byte[] 写入临时文件
            Files.write(tempFile, audioBytes);
            //todo 需要上传到公网地址，通过http/https链接访问
            TranscriptionParam param=TranscriptionParam.builder()
                    .apiKey(credential.getApiKey())
                    .model(modelName)
                    .fileUrls(List.of(""))
                    .parameter("language_hints", new String[] {"en","zh"})
                    .build();
            param.setFileUrls(List.of(""));
            // 提交语音识别任务
            TranscriptionResult result = transcription.asyncCall(param);
            System.out.println("RequestId: " + result.getRequestId());
            // 等待转写完成
            result = transcription.wait(TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
            // 获取语音识别结果
            List<TranscriptionTaskResult> taskResultList = result.getResults();

            if (taskResultList != null && !taskResultList.isEmpty()) {
                for (TranscriptionTaskResult taskResult : taskResultList) {
                    // 获取识别结果的url
                    String transcriptionUrl = taskResult.getTranscriptionUrl();
                    // 获取url内对应的结果
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
            System.out.println("error: " + e);
        }
        return sb.toString();
    }

}
