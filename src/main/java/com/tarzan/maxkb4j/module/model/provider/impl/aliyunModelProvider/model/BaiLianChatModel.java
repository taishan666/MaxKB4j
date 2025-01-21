package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.model.provider.MaxKBBaseModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;

public class BaiLianChatModel extends BaseChatModel implements MaxKBBaseModel {

    @Override
    public <T> T newInstance(String modelName, JSONObject credential) {
        System.out.println("BaiLianChatModel");
        System.out.println("credential"+credential);
        StreamingChatLanguageModel streamingChatModel = QwenStreamingChatModel.builder()
                .apiKey(credential.getString("api_key"))
                .modelName(modelName)
                .build();
        ChatLanguageModel chatModel = QwenChatModel.builder()
                .apiKey(credential.getString("api_key"))
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel,chatModel);
    }
}
