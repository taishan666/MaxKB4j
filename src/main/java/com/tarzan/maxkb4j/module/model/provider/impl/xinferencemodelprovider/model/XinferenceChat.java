package com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.community.model.xinference.XinferenceChatModel;
import dev.langchain4j.community.model.xinference.XinferenceStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

public class XinferenceChat extends BaseChatModel implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        StreamingChatLanguageModel streamingChatModel = XinferenceStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        ChatLanguageModel chatModel = XinferenceChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel, chatModel);
    }
}
