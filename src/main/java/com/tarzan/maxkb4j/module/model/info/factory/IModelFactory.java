package com.tarzan.maxkb4j.module.model.info.factory;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;

/**
 * Interface for model factory implementations
 */
public interface IModelFactory {

    ChatModel buildChatModel(String modelId);

    ChatModel buildChatModel(String modelId, JSONObject modelParams);

    StreamingChatModel buildStreamingChatModel(String modelId, JSONObject modelParams);

    EmbeddingModel buildEmbeddingModel(String modelId);

    EmbeddingModel buildEmbeddingModel(String modelId, JSONObject modelParams);

    ImageModel buildImageModel(String modelId, JSONObject modelParams);

    ScoringModel buildScoringModel(String modelId);

    ScoringModel buildScoringModel(String modelId, JSONObject modelParams);

    TTSModel buildTTSModel(String modelId, JSONObject modelParams);

    STTModel buildSTTModel(String modelId);
}