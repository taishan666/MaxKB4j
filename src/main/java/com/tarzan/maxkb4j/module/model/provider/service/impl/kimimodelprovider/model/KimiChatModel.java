package com.tarzan.maxkb4j.module.model.provider.service.impl.kimimodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialStreamingChatModel;

public class KimiChatModel extends BaseChatModel implements BaseModel<BaseChatModel> {
    @Override
    public BaseChatModel build(String modelName, ModelCredential modelCredential, JSONObject params) {
        OpenAiOfficialChatModel chatModel = new OpenAiOfficialChatModel.Builder()
                .modelName(modelName)
                .apiKey(modelCredential.getApiKey())
                .build();
        OpenAiOfficialStreamingChatModel streamingChatModel = new OpenAiOfficialStreamingChatModel.Builder()
                .modelName(modelName)
                .apiKey(modelCredential.getApiKey())
                .build();
        return new BaseChatModel(streamingChatModel, chatModel);
    }
}
