package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.custom.model.OpenAiSTTModel;
import com.maxkb4j.model.custom.model.OpenAiTTSModel;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.ISTTModel;
import com.maxkb4j.model.service.ITTSModel;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.community.model.xinference.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * XInference Model Provider - Local deployment with OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.X_INFERENCE, name = "Xorbits Inference", icon = Provider.ICON_X_INFERENCE)
public class XInferenceModelProvider extends AbsModelProvider {

    private static final String BASE_URL = BaseUrl.X_INFERENCE;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.QWEN3_8B, "", ModelType.LLM),
            new ModelInfo(ModelName.BGE_M3, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.LLAVA_7B, "", ModelType.VISION),
            new ModelInfo(ModelName.SDXL_TURBO, "", ModelType.TTI),
            new ModelInfo(ModelName.BGE_RERANKER_BASE, "", ModelType.RERANKER),
            new ModelInfo(ModelName.CHAT_TTS, "", ModelType.TTS),
            new ModelInfo(ModelName.WHISPER_LARGE_V3, "", ModelType.STT)
    );


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BASE_URL);
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .maxTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .maxTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceImageModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return XinferenceScoringModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
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
