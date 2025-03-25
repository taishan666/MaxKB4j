package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.listener.LlmListener;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

import java.util.List;

public class BaiLianChatModel extends BaseChatModel implements BaseModel {

    @Override
    public <T> T build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatLanguageModel streamingChatModel = QwenStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .listeners(List.of(new LlmListener()))
                .build();
        ChatLanguageModel chatModel = QwenChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .listeners(List.of(new LlmListener()))
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel,chatModel);
    }

}
