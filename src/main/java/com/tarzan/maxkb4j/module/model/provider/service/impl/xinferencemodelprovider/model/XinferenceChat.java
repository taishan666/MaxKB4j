package com.tarzan.maxkb4j.module.model.provider.service.impl.xinferencemodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.community.model.xinference.XinferenceChatModel;
import dev.langchain4j.community.model.xinference.XinferenceStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public class XinferenceChat extends BaseChatModel implements BaseModel<BaseChatModel> {
    @Override
    public BaseChatModel build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatModel streamingChatModel = XinferenceStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        ChatModel chatModel = XinferenceChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return  new BaseChatModel(streamingChatModel, chatModel);
    }
}
