package com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

public class ZhiPuChatModel extends BaseChatModel implements BaseModel {

    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        StreamingChatLanguageModel streamingChatModel = ZhipuAiStreamingChatModel.builder()
               // .baseUrl(credential.getApiBase())
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
        ChatLanguageModel chatModel = ZhipuAiChatModel.builder()
             //   .baseUrl(credential.getApiBase())
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel,chatModel);
    }
}
