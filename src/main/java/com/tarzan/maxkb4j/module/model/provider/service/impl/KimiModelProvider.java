package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.ArrayList;
import java.util.List;

public class KimiModelProvider extends IModelProvider {

    public final String BASE_URL = "https://api.moonshot.cn/v1";
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.Kimi);
        info.setIcon(getSvgIcon("kimi_icon.svg"));
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("kimi-latest","kimi-latest 模型会根据您请求的上下文长度自动选择对应的计费模型，上下文越长，价格越高", ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("kimi-k2-turbo-preview","",ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("kimi-k2-thinking","",ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("kimi-k2-thinking-turbo","",ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("moonshot-v1-8k-vision-preview","",ModelType.VISION,new LlmModelParams()));
        modelInfos.add(new ModelInfo("moonshot-v1-32k-vision-preview","",ModelType.VISION,new LlmModelParams()));
        modelInfos.add(new ModelInfo("moonshot-v1-128k-vision-preview","",ModelType.VISION,new LlmModelParams()));
        return modelInfos;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .returnThinking(true)
                .build();
    }

}
