package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.params.impl.ImageModelParams;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.custom.params.impl.NoModelParams;
import com.tarzan.maxkb4j.module.model.custom.params.impl.TextEmbeddingV3Params;
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
import java.util.Map;

public class VolcanicEngineModelProvider extends IModelProvider {

    public final String BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo(ModelProviderEnum.VolcanicEngine);
        info.setIcon(getSvgIcon("volcanic_engine_icon.svg"));
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("doubao-1-5-pro-32k-250115","大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("doubao-seed-1-6-251015","大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("doubao-seed-1-6-flash-250828","大语言模型", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("doubao-seed-1-6-thinking-250715","文本向量模型", ModelType.LLM,new NoModelParams()));
        modelInfos.add(new ModelInfo("doubao-seed-1-6-vision-250815","图片理解", ModelType.VISION,new LlmModelParams()));
        modelInfos.add(new ModelInfo("doubao-seedream-4-0-250828","图片生成", ModelType.TTI,new ImageModelParams()));
        modelInfos.add(new ModelInfo("doubao-seedream-4-5-251128","图片生成", ModelType.TTI,new ImageModelParams()));
        modelInfos.add(new ModelInfo("doubao-embedding-text-240715","文本向量模型", ModelType.EMBEDDING,new TextEmbeddingV3Params()));
        return modelInfos;
    }


    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .returnThinking(true)
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

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return  OpenAiImageModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .size("2K")
                .customQueryParams(Map.of("watermark", "false","sequential_image_generation","auto"))
                .build();
    }


}
