package com.tarzan.maxkb4j.module.model.provider.service.impl.ollamamodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

public class OllamaChat extends BaseChatModel implements BaseModel<BaseChatModel> {
    @Override
    public BaseChatModel build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatModel streamingChatModel = OllamaStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return  new BaseChatModel(streamingChatModel, chatModel);
    }
}
