package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.ArrayList;
import java.util.List;

public class OpenaiModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.OpenAI);
        info.setIcon(getSvgIcon("openai_icon.svg"));
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("gpt-3.5-turbo","", ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4","",ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o","",ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o-mini","",ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4-turbo","",ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4-turbo-preview","",ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-ada-002","",ModelType.EMBEDDING));
/*        modelInfos.add(new ModelInfo("whisper-1","",ModelType.STT));
        modelInfos.add(new ModelInfo("tts-1","",ModelType.TTS));*/
        modelInfos.add(new ModelInfo("gpt-4o","",ModelType.VISION,new LlmModelParams()));
        modelInfos.add(new ModelInfo("dall-e-2","",ModelType.TTI));
        return modelInfos;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
       return new ModelCredentialForm(true,true);
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return  OpenAiEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return  OpenAiImageModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }


}
