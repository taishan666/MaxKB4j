package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.listener.LlmListener;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.List;

public class BaiLianChatModel extends BaseChatModel implements BaseModel<BaseChatModel> {

    @Override
    public BaseChatModel build(String modelName, ModelCredential credential, JSONObject params) {
        StreamingChatModel streamingChatModel = QwenStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(params==null?null:params.getFloat("temperature"))
                .maxTokens(params==null?null:params.getInteger("max_tokens"))
                .listeners(List.of(new LlmListener()))
                .build();
        ChatModel chatModel = QwenChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(params==null?null:params.getFloat("temperature"))
                .maxTokens(params==null?null:params.getInteger("max_tokens"))
                .listeners(List.of(new LlmListener()))
                .build();
        // 使用构造函数实例化对象
        return  new BaseChatModel(streamingChatModel,chatModel);
    }

}
