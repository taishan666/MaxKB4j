package com.tarzan.maxkb4j.module.model.provider.impl.deepseekmodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

public class DeepSeekChatModel extends BaseChatModel implements BaseModel<BaseChatModel> {

    @Override
    public BaseChatModel build(String modelName, ModelCredential credential, JSONObject params) {
        String baseUrl = "https://api.deepseek.com/v1";
        StreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .returnThinking(true)
               // .listeners(List.of(new LlmListener()))
                .build();
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
               // .listeners(List.of(new LlmListener()))
                .build();
        // 使用构造函数实例化对象
        return new BaseChatModel(streamingChatModel,chatModel);
    }
}
