package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.integration.credential.ModelCredentialForm;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Azure OpenAI Model Provider
 */
@Component
@ModelProviderType(provider = Provider.AZURE, name = "Azure OpenAI", icon = Provider.ICON_AZURE)
public class AzureModelProvider extends AbsModelProvider {


    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BaseUrl.AZURE);
    }


    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.AZURE;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiChatModel.builder()
                .apiKey(credential.getApiKey())
                .endpoint(getEndpoint(credential.getBaseUrl()))
                .deploymentName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .maxTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .endpoint(getEndpoint(credential.getBaseUrl()))
                .deploymentName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .maxTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .endpoint(getEndpoint(credential.getBaseUrl()))
                .deploymentName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return AzureOpenAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .endpoint(getEndpoint(credential.getBaseUrl()))
                .deploymentName(modelName)
                .build();
    }

    public String getEndpoint(String baseUrl) {
        return StringUtils.isNotBlank(baseUrl) ? baseUrl : BaseUrl.AZURE;
    }
}
