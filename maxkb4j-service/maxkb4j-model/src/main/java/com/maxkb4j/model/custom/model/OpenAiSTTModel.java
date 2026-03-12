package com.maxkb4j.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.service.STTModel;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;
import lombok.Data;

@Data
public class OpenAiSTTModel implements STTModel {

    private OpenAIClient client;
    private String modelName;
    private JSONObject params;

    public OpenAiSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder();
        builder.baseUrl(credential.getBaseUrl());
        builder.apiKey(credential.getApiKey());
        this.client= builder.build();
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
