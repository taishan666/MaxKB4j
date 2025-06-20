package com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

public class OpenaiChatModel extends BaseChatModel implements BaseModel<BaseChatModel> {

    @Override
    public BaseChatModel build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
               // .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        ChatModel chatModel = OpenAiChatModel.builder()
             //   .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return  new BaseChatModel(streamingChatModel,chatModel);
    }
}
