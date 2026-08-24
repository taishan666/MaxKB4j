package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.custom.model.OpenAiSTTModel;
import com.maxkb4j.model.custom.model.OpenAiTTSModel;
import com.maxkb4j.model.custom.params.OpenAiChatModelParams;
import com.maxkb4j.model.custom.params.OpenAiImageModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.ISTTModel;
import com.maxkb4j.model.service.ITTSModel;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * OpenAI Model Provider Implementation
 * Provides integration with OpenAI's API services
 */
@Component
@ModelProviderType(provider = Provider.OPENAI, name = "OpenAI", icon = Provider.ICON_OPENAI)
public class OpenAiModelProvider extends AbsModelProvider {
    private static final String BASE_URL = BaseUrl.OPENAI;

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.GPT_3_5_TURBO, "GPT-3.5 Turbo", ModelType.LLM),
            new ModelInfo(ModelName.GPT_4, "GPT-4", ModelType.LLM),
            new ModelInfo(ModelName.GPT_4O, "GPT-4 Omni", ModelType.LLM),
            new ModelInfo(ModelName.GPT_4O_MINI, "GPT-4 Omni Mini", ModelType.LLM),
            new ModelInfo(ModelName.GPT_4_TURBO, "GPT-4 Turbo", ModelType.LLM),
            new ModelInfo(ModelName.GPT_4_TURBO_PREVIEW, "GPT-4 Turbo Preview", ModelType.LLM),
            new ModelInfo(ModelName.TEXT_EMBEDDING_ADA_002, "Text Embedding Ada v2", ModelType.EMBEDDING),
            new ModelInfo(ModelName.WHISPER_1, "Whisper Speech-to-Text", ModelType.STT),
            new ModelInfo(ModelName.TTS_1, "Text-to-Speech", ModelType.TTS),
            new ModelInfo(ModelName.GPT_4O, "GPT-4 Vision", ModelType.VISION),
            new ModelInfo(ModelName.DALLE_2, "DALL·E 2", ModelType.TTI)
    );


    public String getDefaultBaseUrl(){
        return BASE_URL;
    }

    public String getBaseUrl(String baseUrl){
        return StringUtils.isNotBlank(baseUrl)?baseUrl:getDefaultBaseUrl();
    }
    @Override
    public List<BaseField> getChatModelParamsForm() {
        return new OpenAiChatModelParams().toForm();
    }

    @Override
    public List<BaseField> getImageModelParamsForm() {
        return new OpenAiImageModelParams().toForm();
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }



    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, getDefaultBaseUrl());
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        boolean enableThinking = getBooleanParam(params, ParamKey.ENABLE_THINKING);
        String flag = enableThinking ? Value.ENABLED : Value.DISABLED;
        params.put(ParamKey.THINKING, Map.of(ParamKey.TYPE, flag));
        return OpenAiChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(getBaseUrl(credential.getBaseUrl()))
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .customParameters(params)
                .sendThinking(true)
                .returnThinking(true)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(getBaseUrl(credential.getBaseUrl()))
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .customParameters(params)
                .sendThinking(true)
                .returnThinking(true)
               // .strictJsonSchema(true)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiEmbeddingModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(getBaseUrl(credential.getBaseUrl()))
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .dimensions(getIntParam(params, ParamKey.DIMENSIONS))
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiImageModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .size(getStringParam(params, ParamKey.SIZE))
                .quality(getStringParam(params, ParamKey.QUALITY))
                .build();
    }

    @Override
    public ISTTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new OpenAiSTTModel(modelName, credential, params);
    }

    @Override
    public ITTSModel buildTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        return new OpenAiTTSModel(modelName, credential, params);
    }
}
