package com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

public class OpenaiChatModel extends BaseChatModel implements BaseModel {

    @Override
    public <T> T build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatLanguageModel streamingChatModel = OpenAiStreamingChatModel.builder()
               // .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
             //   .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel,chatModel);
    }
}
