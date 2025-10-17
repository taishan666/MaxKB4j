package com.tarzan.maxkb4j.module.model.provider.service.impl.azuremodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.image.ImageModel;

public class AzureOpenaiImageModel implements BaseModel<ImageModel> {

    @Override
    public AzureOpenAiImageModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  AzureOpenAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName).build();
    }




}
