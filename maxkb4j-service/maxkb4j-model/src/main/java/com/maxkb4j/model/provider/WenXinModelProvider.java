package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.community.model.qianfan.QianfanChatModel;
import dev.langchain4j.community.model.qianfan.QianfanEmbeddingModel;
import dev.langchain4j.community.model.qianfan.QianfanStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

/**
 * WenXin (Baidu Qianfan) Model Provider
 */
@Component
@ModelProviderType(provider = "WenXin", name = "文心一言", icon = "wenxin_icon.svg")
public class WenXinModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("ERNIE-Bot-4", "", ModelType.LLM),
            new ModelInfo("ERNIE-Bot", "", ModelType.LLM),
            new ModelInfo("ERNIE-Bot-turbo", "", ModelType.LLM),
            new ModelInfo("Embedding-V1", "", ModelType.EMBEDDING)
    );

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return QianfanChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(params.getInteger("max_tokens"))
                .temperature(params.getDouble("temperature"))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return QianfanStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(params.getInteger("max_tokens"))
                .temperature(params.getDouble("temperature"))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return QianfanEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
