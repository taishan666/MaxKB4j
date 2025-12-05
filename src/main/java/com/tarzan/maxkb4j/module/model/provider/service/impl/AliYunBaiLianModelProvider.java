package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.listener.LlmListener;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.custom.model.*;
import com.tarzan.maxkb4j.module.model.custom.params.impl.*;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.community.model.dashscope.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;

import java.util.ArrayList;
import java.util.List;

public class AliYunBaiLianModelProvider extends IModelProvider {

    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.AliYunBaiLian);
        info.setIcon(getSvgIcon("qwen_icon.svg"));
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_TURBO,"大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_PLUS,"大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_MAX,"大语言模型", ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-v4","文本向量模型", ModelType.EMBEDDING,new TextEmbeddingV4Params()));
        modelInfos.add(new ModelInfo("text-embedding-v3","文本向量模型", ModelType.EMBEDDING,new TextEmbeddingV3Params()));
        modelInfos.add(new ModelInfo("paraformer-realtime-v2","语音识别模型", ModelType.STT));
        modelInfos.add(new ModelInfo("fun-asr-realtime","语音识别模型", ModelType.STT));
        modelInfos.add(new ModelInfo("gummy-realtime-v1","语音识别模型", ModelType.STT, GummySTT.class,new GummySTTParams()));
        modelInfos.add(new ModelInfo("cosyvoice-v1","语音生成模型", ModelType.TTS,CosyVoiceTTS.class,new CosyVoiceV1TTSParams()));
        modelInfos.add(new ModelInfo("cosyvoice-v2","语音生成模型", ModelType.TTS,CosyVoiceTTS.class,new CosyVoiceV2TTSParams()));
        modelInfos.add(new ModelInfo("sambert-v1","语音生成模型", ModelType.TTS,SamBertTTS.class,new SamBertTTSParams()));
        modelInfos.add(new ModelInfo("qwen-tts","语音生成模型", ModelType.TTS,QWenTTS.class,new QWenTTSParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_VL_PLUS,"AI视觉模型", ModelType.VISION, new LlmModelParams()));
        modelInfos.add(new ModelInfo(QwenModelName.QWEN_VL_MAX,"AI视觉模型", ModelType.VISION, new LlmModelParams()));
        modelInfos.add(new ModelInfo(WanxModelName.WANX2_1_T2I_TURBO,"文生图模型", ModelType.TTI,new WanXImageModelParams()));
        modelInfos.add(new ModelInfo(WanxModelName.WANX2_1_T2I_PLUS,"文生图模型", ModelType.TTI,new WanXImageModelParams()));
        modelInfos.add(new ModelInfo("qwen-image-plus","文生图模型", ModelType.TTI,new QwenImageModelParams()));
        modelInfos.add(new ModelInfo("gte-rerank","重排模型", ModelType.RERANKER));
        return modelInfos;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return QwenChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .isMultimodalModel(params==null?null:params.getBoolean("isMultimodalModel"))
                .temperature(params==null?null:params.getFloat("temperature"))
                .maxTokens(params==null?null:params.getInteger("maxTokens"))
                .listeners(List.of(new LlmListener()))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return  QwenStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .isMultimodalModel(params==null?null:params.getBoolean("isMultimodalModel"))
                .temperature(params==null?null:params.getFloat("temperature"))
                .maxTokens(params==null?null:params.getInteger("maxTokens"))
                .listeners(List.of(new LlmListener()))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return  QwenEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .dimension(params.getInteger("dimension"))
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return WanxImageModel.builder()
                .modelName(modelName)
                .apiKey(credential.getApiKey())
                .size(WanxImageSize.of(params.getString("size")))
                .style(WanxImageStyle.of(params.getString("style")))
                .negativePrompt(params.getString("negative_prompt"))
                .promptExtend(params.getBoolean("prompt_extend"))
                .watermark(params.getBoolean("watermark"))
                .seed(params.getInteger("seed"))
                .build();
    }

    @Override
    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianReranker(modelName,credential,params);
    }

    @Override
    public STTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianSTTModel(modelName,credential,params);
    }

}
