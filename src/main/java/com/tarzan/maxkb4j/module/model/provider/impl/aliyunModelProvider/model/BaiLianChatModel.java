package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class BaiLianChatModel extends BaseChatModel implements BaseModel {

    @Override
    public <T> T newInstance(String modelName, JSONObject credential) {
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
