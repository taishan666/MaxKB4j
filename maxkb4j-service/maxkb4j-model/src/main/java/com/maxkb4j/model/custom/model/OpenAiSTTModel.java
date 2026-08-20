package com.maxkb4j.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.service.ISTTModel;
import com.openai.client.OpenAIClient;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;
import lombok.Data;

@Data
public class OpenAiSTTModel implements ISTTModel {

    private OpenAIClient client;
    private String modelName;
    private JSONObject params;

    public OpenAiSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        // 复用客户端，避免每次调用新建 OkHttp 连接池与线程池
        this.client = OpenAiClientHolder.getOrCreate(credential);
        this.modelName = modelName;
        this.params = params;
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        TranscriptionCreateParams transcriptionCreateParams = TranscriptionCreateParams.builder()
                .model(modelName)
                .file(audioBytes)
                .build();
        TranscriptionCreateResponse chatCompletion = client.audio().transcriptions().create(transcriptionCreateParams);
        return chatCompletion.asTranscription().text();
    }
}
