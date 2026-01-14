package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.ArrayList;
import java.util.List;

public class TencentModelProvider extends IModelProvider {

    public final String BASE_URL = "https://api.hunyuan.cloud.tencent.com/v1";
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.Tencent);
        info.setIcon(getSvgIcon("tencent_icon.svg"));
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("hunyuan-pro","", ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-standard","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-lite","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-role","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-functioncall","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-code","",ModelType.LLM));
        modelInfos.add(new ModelInfo("hunyuan-embedding","",ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("hunyuan-vision","",ModelType.VISION));
        modelInfos.add(new ModelInfo("hunyuan-dit","", ModelType.TTI));
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

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

}
