package com.tarzan.maxkb4j.module.model.provider.service.impl.zhipumodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public class ZhiPuChatModel extends BaseChatModel implements BaseModel<BaseChatModel> {

    @Override
    public BaseChatModel build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatModel streamingChatModel = ZhipuAiStreamingChatModel.builder()
               // .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
        ChatModel chatModel = ZhipuAiChatModel.builder()
             //   .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
        // 使用构造函数实例化对象
        return new BaseChatModel(streamingChatModel,chatModel);
    }
}
