package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

public class BaiLianChatModel extends BaseChatModel implements BaseModel {

    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        StreamingChatLanguageModel streamingChatModel = QwenStreamingChatModel.builder()
               // .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        ChatLanguageModel chatModel = QwenChatModel.builder()
             //   .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel,chatModel);
    }
}
