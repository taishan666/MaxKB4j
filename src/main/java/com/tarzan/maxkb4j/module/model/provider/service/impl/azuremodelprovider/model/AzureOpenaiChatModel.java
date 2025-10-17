package com.tarzan.maxkb4j.module.model.provider.service.impl.azuremodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public class AzureOpenaiChatModel extends BaseChatModel implements BaseModel<BaseChatModel> {

    @Override
    public BaseChatModel build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatModel streamingChatModel = AzureOpenAiStreamingChatModel.builder()
               // .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
        ChatModel chatModel = AzureOpenAiChatModel.builder()
             //   .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
        // 使用构造函数实例化对象
        return new BaseChatModel(streamingChatModel,chatModel);
    }
}
