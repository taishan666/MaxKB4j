package com.maxkb4j.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.service.ITTSModel;
import com.openai.client.OpenAIClient;
import com.openai.core.http.HttpResponse;
import com.openai.models.audio.speech.SpeechCreateParams;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;

@Data
public class OpenAiTTSModel implements ITTSModel {

    private OpenAIClient client;
    private String modelName;
    private JSONObject params;

    public OpenAiTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        // 复用客户端，避免每次调用新建 OkHttp 连接池与线程池
        this.client = OpenAiClientHolder.getOrCreate(credential);
        this.modelName = modelName;
        this.params = params;
    }
    @Override
    public byte[] textToSpeech(String text){
        String voice= params.getString("voice");
        Float speechRate=params.getFloat("speechRate");
        SpeechCreateParams speechCreateParams = SpeechCreateParams.builder()
                .model(modelName)
                .input(text)
                .speed(speechRate)
                .voice(voice)
                .build();
        HttpResponse httpResponse = client.audio().speech().create(speechCreateParams);
        // 响应体流必须关闭，否则 OkHttp 连接无法归还连接池
        try (InputStream body = httpResponse.body()) {
            return body.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
