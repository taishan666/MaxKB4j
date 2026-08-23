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
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * WenXin (Baidu Qianfan) Model Provider
 */
@Component
@ModelProviderType(provider = Provider.WEN_XIN, name = "文心一言", icon = Provider.ICON_WEN_XIN)
public class WenXinModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.ERNIE_BOT_4, "", ModelType.LLM),
            new ModelInfo(ModelName.ERNIE_BOT, "", ModelType.LLM),
            new ModelInfo(ModelName.ERNIE_BOT_TURBO, "", ModelType.LLM),
            new ModelInfo(ModelName.EMBEDDING_V1, "", ModelType.EMBEDDING)
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
                .maxOutputTokens(params.getInteger(ParamKey.MAX_TOKENS))
                .temperature(params.getDouble(ParamKey.TEMPERATURE))
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return QianfanStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .maxOutputTokens(params.getInteger(ParamKey.MAX_TOKENS))
                .temperature(params.getDouble(ParamKey.TEMPERATURE))
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
