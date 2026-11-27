package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.community.model.qianfan.QianfanChatModel;
import dev.langchain4j.community.model.qianfan.QianfanEmbeddingModel;
import dev.langchain4j.community.model.qianfan.QianfanStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.ParamKey;
import static com.maxkb4j.model.consts.ModelConstants.Provider;

/**
 * WenXin (Baidu Qianfan) Model Provider
 */
@Component
@ModelProviderType(provider = Provider.WEN_XIN, name = "文心一言", icon = Provider.ICON_WEN_XIN)
public class WenXinModelProvider extends AbsModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.WEN_XIN;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return QianfanChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return QianfanStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(getIntParam(params, ParamKey.MAX_TOKENS))
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
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
