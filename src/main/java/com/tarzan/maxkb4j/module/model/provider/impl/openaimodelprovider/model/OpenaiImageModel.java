package com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.openai.OpenAiImageModel;

public class OpenaiImageModel implements BaseModel {

    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) OpenAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName).build();
    }




}
