package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.dashscope.common.TaskStatus;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class SenseVoiceSTT extends BaseSpeechToText implements BaseModel<BaseSpeechToText> {

    private TranscriptionParam param;
    @Override
    public BaseSpeechToText build(String modelName, ModelCredential modelCredential, JSONObject params) {
        this.param = TranscriptionParam.builder()
                        .apiKey(modelCredential.getApiKey())
                        .model(modelName)
                        .fileUrls(List.of("https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/sensevoice/rich_text_example_1.wav"))
                        .parameter("language_hints", new String[] {"en","zh"})
                        .build();
        return this;
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        try {
            Transcription transcription = new Transcription();
            // 创建临时文件
            Path tempFile= Files.createTempFile("temp_audio_"+System.currentTimeMillis(),("."+suffix));
            // 将 byte[] 写入临时文件
            Files.write(tempFile, audioBytes);
            //  String fileUrl = OSSUtils.upload(param.getModel(), tempFile.toFile().getPath(), param.getApiKey());
            param.setFileUrls(List.of("file://"+tempFile.toFile().getPath()));
            // 提交语音识别任务
            TranscriptionResult result = transcription.asyncCall(param);
            System.out.println("RequestId: " + result.getRequestId());
            // 循环获取任务执行结果，直到任务结束
            while (true) {
                result = transcription.fetch(TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
                if (result.getTaskStatus() == TaskStatus.SUCCEEDED || result.getTaskStatus() == TaskStatus.FAILED) {
                    break;
                }
                Thread.sleep(500L);
            }
            // 获取语音识别结果
            List<TranscriptionTaskResult> taskResultList = result.getResults();
            if (taskResultList != null && !taskResultList.isEmpty()) {
                TranscriptionTaskResult taskResult = taskResultList.get(0);
                // 获取识别结果的url
                String transcriptionUrl = taskResult.getTranscriptionUrl();
                // 获取url内对应的结果
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(transcriptionUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                String response = HttpUtil.get(transcriptionUrl);
                JSONObject json = JSONObject.parseObject(response);
                return json.getJSONArray("transcripts").getJSONObject(0).getString("text");
            }
        } catch (Exception e) {
            System.out.println("error: " + e);
        }
        return "";
    }

}
