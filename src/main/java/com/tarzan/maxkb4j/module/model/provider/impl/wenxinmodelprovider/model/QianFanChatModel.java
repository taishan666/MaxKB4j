package com.tarzan.maxkb4j.module.model.provider.impl.wenxinmodelprovider.model;

import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.community.model.qianfan.QianfanChatModel;
import dev.langchain4j.community.model.qianfan.QianfanStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

public class QianFanChatModel extends BaseChatModel implements BaseModel {

    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        StreamingChatLanguageModel streamingChatModel = QianfanStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        ChatLanguageModel chatModel = QianfanChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
        // 使用构造函数实例化对象
        return (T) new BaseChatModel(streamingChatModel,chatModel);
    }
}
