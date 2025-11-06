package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_gemini_provider")
public class GeminiModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.Gemini.getProvider());
        info.setName(ModelProviderEnum.Gemini.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/gemini_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }


    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("gemini-1.0-pro","","LLM"));
        modelInfos.add(new ModelInfo("gemini-1.0-pro-visio","","LLM"));
        modelInfos.add(new ModelInfo("models/embedding-001","","EMBEDDING"));
        modelInfos.add(new ModelInfo("gemini-1.5-flash","","STT"));
        modelInfos.add(new ModelInfo("gemini-1.5-flash","","IMAGE"));
        return modelInfos;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return  GoogleAiGeminiChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                 .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return  GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return  GoogleAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }


}
