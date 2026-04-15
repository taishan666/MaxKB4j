package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.custom.model.BaiLianImageModel;
import com.maxkb4j.model.custom.model.BaiLianReranker;
import com.maxkb4j.model.custom.model.BaiLianSTTModel;
import com.maxkb4j.model.custom.model.BaiLianTTSModel;
import com.maxkb4j.model.custom.params.impl.*;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.STTModel;
import com.maxkb4j.model.service.TTSModel;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.community.model.dashscope.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;

import java.util.List;

/**
 * AliYun BaiLian (DashScope) Model Provider
 */
public class AliYunBaiLianModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(QwenModelName.QWEN_TURBO, "", ModelType.LLM, new QWenChatModelParams()),
            new ModelInfo("qwen3.5-plus", "", ModelType.LLM, new QWenChatModelParams(true)),
            new ModelInfo(QwenModelName.QWEN_PLUS, "", ModelType.LLM, new QWenChatModelParams()),
            new ModelInfo(QwenModelName.QWEN_MAX, "", ModelType.LLM, new QWenChatModelParams()),
            new ModelInfo("text-embedding-v4", "", ModelType.EMBEDDING, new TextEmbeddingV4Params()),
            new ModelInfo("text-embedding-v3", "", ModelType.EMBEDDING, new TextEmbeddingV3Params()),
            new ModelInfo("paraformer-realtime-v2", "", ModelType.STT),
            new ModelInfo("fun-asr-realtime", "", ModelType.STT),
            new ModelInfo("gummy-realtime-v1", "", ModelType.STT, new GummySTTParams()),
            new ModelInfo("cosyvoice-v1", "", ModelType.TTS, new CosyVoiceV1TTSParams()),
            new ModelInfo("cosyvoice-v2", "", ModelType.TTS, new CosyVoiceV2TTSParams()),
            new ModelInfo("sambert-v1", "", ModelType.TTS, new SamBertTTSParams()),
            new ModelInfo("qwen3-tts-flash", "", ModelType.TTS, new QWenTTSParams()),
            new ModelInfo("qwen-tts", "", ModelType.TTS, new QWenTTSParams()),
            new ModelInfo("qwen3.5-plus", "", ModelType.VISION, new QWenChatModelParams(true)),
            new ModelInfo(QwenModelName.QWEN_VL_PLUS, "", ModelType.VISION, new QWenChatModelParams(true)),
            new ModelInfo(QwenModelName.QWEN_VL_MAX, "", ModelType.VISION, new QWenChatModelParams(true)),
            new ModelInfo(WanxModelName.WANX2_1_T2I_TURBO, "", ModelType.TTI, new WanXImageModelParams()),
            new ModelInfo(WanxModelName.WANX2_1_T2I_PLUS, "", ModelType.TTI, new WanXImageModelParams()),
            new ModelInfo("qwen-image-plus", "", ModelType.TTI, new QwenImageModelParams()),
            new ModelInfo("gte-rerank", "", ModelType.RERANKER)
    );


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return QwenChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getFloatParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .isMultimodalModel(getBooleanParam(params, "isMultimodalModel"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return QwenStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(getFloatParam(params, "temperature"))
                .maxTokens(getIntParam(params, "maxTokens"))
                .isMultimodalModel(getBooleanParam(params, "isMultimodalModel"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return QwenEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .dimension(getIntParam(params, "dimension"))
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianImageModel(modelName, credential, params);
    }

    @Override
    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianReranker(modelName, credential, params);
    }

    @Override
    public STTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianSTTModel(modelName, credential, params);
    }

    @Override
    public TTSModel buildTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianTTSModel(modelName, credential, params);
    }
}
