package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class AzureModelProvider  extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.Azure.getProvider());
        info.setName(ModelProviderEnum.Azure.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/azure_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("Azure OpenAI","", ModelType.LLM.name(), new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4","", ModelType.LLM.name(), new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o","", ModelType.LLM.name(),new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o-mini","", ModelType.LLM.name(), new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-3-large","", ModelType.EMBEDDING.name()));
        modelInfos.add(new ModelInfo("text-embedding-3-small","", ModelType.EMBEDDING.name()));
        modelInfos.add(new ModelInfo("text-embedding-ada-002","", ModelType.EMBEDDING.name()));
        modelInfos.add(new ModelInfo("gpt-4o","", ModelType.IMAGE_UNDERSTANDING.name(), new LlmModelParams()));
        modelInfos.add(new ModelInfo("gpt-4o-mini","", ModelType.IMAGE_UNDERSTANDING.name(), new LlmModelParams()));
        modelInfos.add(new ModelInfo("dall-e-3","", ModelType.TTI.name()));
        return modelInfos;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiChatModel.builder()
                //   .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return  AzureOpenAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName).build();
    }


}
