package com.tarzan.maxkb4j.module.model.provider.impl.ollamamodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

public class OllamaChat extends BaseChatModel implements BaseModel {
    @Override
    public <T> T build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatLanguageModel streamingChatModel = OllamaStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
        ChatLanguageModel chatModel = OllamaChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel, chatModel);
    }
}
