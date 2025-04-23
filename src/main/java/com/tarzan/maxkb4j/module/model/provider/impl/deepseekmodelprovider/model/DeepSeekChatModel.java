package com.tarzan.maxkb4j.module.model.provider.impl.deepseekmodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

public class DeepSeekChatModel extends BaseChatModel implements BaseModel {

    @Override
    public <T> T build(String modelName, ModelCredential credential, JSONObject params) {
        String baseUrl = "https://api.deepseek.com/v1";
        StreamingChatLanguageModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
               // .listeners(List.of(new LlmListener()))
                .build();
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
               // .listeners(List.of(new LlmListener()))
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel,chatModel);
    }
}
