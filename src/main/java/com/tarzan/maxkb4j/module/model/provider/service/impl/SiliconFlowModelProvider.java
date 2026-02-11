package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.custom.credential.ModelCredentialForm;
import com.tarzan.maxkb4j.module.model.custom.model.OpenAiSTTModel;
import com.tarzan.maxkb4j.module.model.custom.model.OpenAiTTSModel;
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

/**
 * OpenAI Model Provider Implementation
 * Provides integration with OpenAI's API services
 */
public class SiliconFlowModelProvider extends IModelProvider {

    private final static String BASE_URL = "https://api.siliconflow.cn/v1";

    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.SiliconFlow);
        info.setIcon(getSvgIcon("silicon_flow_icon.svg"));
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("deepseek-ai/DeepSeek-V3.2", "", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("Pro/moonshotai/Kimi-K2.5", "", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("Qwen/Qwen3-VL-32B-Thinking", "", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("Pro/zai-org/GLM-4.7", "", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("Pro/MiniMaxAI/MiniMax-M2.1", "", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("tencent/Hunyuan-MT-7B", "", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("Qwen/Qwen3-Embedding-8B", "", ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("BAAI/bge-m3", "", ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("netease-youdao/bce-embedding-base_v1", "", ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("Qwen/Qwen3-Reranker-8B", "", ModelType.RERANKER));
        modelInfos.add(new ModelInfo("BAAI/bge-reranker-v2-m3", "", ModelType.RERANKER));
        modelInfos.add(new ModelInfo("netease-youdao/bce-reranker-base_v1", "", ModelType.RERANKER));
        modelInfos.add(new ModelInfo("Qwen/Qwen3-VL-32B-Thinking", "", ModelType.VISION, new LlmModelParams()));
        modelInfos.add(new ModelInfo("Qwen/Qwen-Image", "", ModelType.TTI));
        return modelInfos;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(true, BASE_URL);
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(params.getDouble("temperature"))
                .maxTokens(params.getInteger("maxTokens"))
                .returnThinking(params.getBoolean("enableThinking"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .temperature(params.getDouble("temperature"))
                .maxTokens(params.getInteger("maxTokens"))
                .returnThinking(params.getBoolean("enableThinking"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiImageModel.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .size(params.getString("size"))
                .quality(params.getString("quality"))
                .style(params.getString("style"))
                .build();
    }

    @Override
    public STTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new OpenAiSTTModel(modelName, credential, params);
    }

    @Override
    public TTSModel buildTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        return new OpenAiTTSModel(modelName, credential, params);
    }
}
