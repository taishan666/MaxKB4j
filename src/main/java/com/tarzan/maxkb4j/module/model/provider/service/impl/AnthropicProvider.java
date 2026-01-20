package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.ArrayList;
import java.util.List;

public class AnthropicProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.Anthropic);
        info.setIcon(getSvgIcon("anthropic_icon.svg"));
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("claude-3-opus-20240229","大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("claude-3-sonnet-20240229","大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("claude-3-haiku-20240307","大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("claude-3-5-sonnet-20241022","大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("claude-3-5-haiku-20241022","大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("claude-3-5-sonnet-20241022","AI视觉模型", ModelType.VISION, new LlmModelParams()));
        return modelInfos;
    }


    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AnthropicChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AnthropicStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

}
