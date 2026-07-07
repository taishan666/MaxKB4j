package com.maxkb4j.model.service;


import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;

/**
 * Interface for model factory implementations
 */
public interface IModelProviderService {

    default ChatModel buildChatModel(String modelId) {
        return buildChatModel(modelId, new JSONObject());
    }

    ChatModel buildChatModel(String modelId, JSONObject modelParams);

    default StreamingChatModel buildStreamingChatModel(String modelId) {
        return buildStreamingChatModel(modelId, new JSONObject());
    }

    StreamingChatModel buildStreamingChatModel(String modelId, JSONObject modelParams);

    default EmbeddingModel buildEmbeddingModel(String modelId) {
        return buildEmbeddingModel(modelId, new JSONObject());
    }

    EmbeddingModel buildEmbeddingModel(String modelId, JSONObject modelParams);

    ImageModel buildImageModel(String modelId, JSONObject modelParams);

    ScoringModel buildScoringModel(String modelId);

    ITTSModel buildTTSModel(String modelId, JSONObject modelParams);

    ISTTModel buildSTTModel(String modelId, JSONObject modelParams);
}