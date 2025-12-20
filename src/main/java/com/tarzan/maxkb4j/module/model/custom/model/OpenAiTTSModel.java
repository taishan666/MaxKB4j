package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.HttpResponse;
import com.openai.models.audio.speech.SpeechCreateParams;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import lombok.Data;

import java.io.IOException;

@Data
public class OpenAiTTSModel implements TTSModel {

    private OpenAIClient client;
    private String modelName;
    private JSONObject params;

    public OpenAiTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder();
        builder.baseUrl(credential.getBaseUrl());
        builder.apiKey(credential.getApiKey());
        this.client= builder.build();
        this.modelName = modelName;
        this.params = params;
    }
    @Override
    public byte[] textToSpeech(String text){
        String voice= params.getString("voice");
        Integer volume= params.getInteger("volume");
        Float speechRate=params.getFloat("speechRate");
        SpeechCreateParams speechCreateParams = SpeechCreateParams.builder()
                .model(modelName)
                .input(text)
                .speed(speechRate)
                .voice(voice)
                .build();
        HttpResponse httpResponse = client.audio().speech().create(speechCreateParams);
        try {
            return httpResponse.body().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
