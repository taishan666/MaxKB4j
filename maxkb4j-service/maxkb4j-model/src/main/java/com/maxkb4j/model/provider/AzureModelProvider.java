package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Azure OpenAI Model Provider
 */
@Component
@ModelProviderType(provider = Provider.AZURE, name = "Azure OpenAI", icon = Provider.ICON_AZURE)
public class AzureModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("Azure OpenAI", "", ModelType.LLM),
            new ModelInfo(ModelName.GPT_4, "", ModelType.LLM),
            new ModelInfo(ModelName.GPT_4O, "", ModelType.LLM),
            new ModelInfo(ModelName.GPT_4O_MINI, "", ModelType.LLM),
            new ModelInfo(ModelName.TEXT_EMBEDDING_3_LARGE, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.TEXT_EMBEDDING_3_SMALL, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.TEXT_EMBEDDING_ADA_002, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.GPT_4O, "", ModelType.VISION),
            new ModelInfo(ModelName.GPT_4O_MINI, "", ModelType.VISION),
            new ModelInfo(ModelName.DALLE_3, "", ModelType.TTI)
    );


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiChatModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .maxTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .maxTokens(getIntParam(params, ParamKey.MAX_TOKENS))
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
        return AzureOpenAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .deploymentName(modelName)
                .build();
    }
}
