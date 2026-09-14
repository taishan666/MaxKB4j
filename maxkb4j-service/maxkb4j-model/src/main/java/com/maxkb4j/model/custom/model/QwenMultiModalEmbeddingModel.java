package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.embeddings.*;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingInput;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.dashscope.embeddings.TextEmbedding.Models.TEXT_EMBEDDING_V1;
import static com.alibaba.dashscope.embeddings.TextEmbedding.Models.TEXT_EMBEDDING_V2;


public class QwenMultiModalEmbeddingModel extends AbstractMultiModalEmbeddingModel {

    private final String apiKey;
    private final MultiModalEmbedding embedding;
    private final QwenEmbeddingModel qwenEmbeddingModel;

    public QwenMultiModalEmbeddingModel(String baseUrl, String apiKey, String modelName, Integer dimension) {
        super(Utils.isNullOrBlank(modelName) ? QwenModelName.TEXT_EMBEDDING_V4 : modelName);
        if (Utils.isNullOrBlank(apiKey)) {
            throw new IllegalArgumentException(
                    "DashScope api key must be defined. Reference: https://www.alibabacloud.com/help/en/model-studio/get-api-key");
        }
        this.apiKey = apiKey;
        this.dimension = ensureDimension(this.modelName, dimension);
        this.embedding = new MultiModalEmbedding();
        this.qwenEmbeddingModel = new QwenEmbeddingModel(baseUrl, apiKey, modelName, dimension);
    }

    private static Embedding toEmbedding(MultiModalEmbeddingOutput output) {
        List<MultiModalEmbeddingResultItem> embeddings = output.getEmbeddings();
        if (embeddings.isEmpty()) {
            throw new IllegalArgumentException("Multi-modal embedding response contains no embedding vector");
        }
        return Embedding.from(embeddings.getFirst().getEmbedding().stream().map(Double::floatValue).toList());
    }

    private static Integer ensureDimension(String modelName, Integer dimension) {
        if (dimension == null) {
            return null;
        }
        if (TEXT_EMBEDDING_V1.equals(modelName) || TEXT_EMBEDDING_V2.equals(modelName)) {
            throw new IllegalArgumentException("dimension '" + dimension + "' is not supported by " + modelName);
        }
        return dimension;
    }

    public static QwenMultiModalEmbeddingModel.Builder builder() {
        return new QwenMultiModalEmbeddingModel.Builder();
    }

    @Override
    protected DimensionAwareEmbeddingModel textEmbeddingModel() {
        return qwenEmbeddingModel;
    }

    @Override
    protected EmbeddingResult embedMultimodal(EmbeddingInput input) {
        List<MultiModalEmbeddingItemBase> contents = toContents(input);
        MultiModalEmbeddingParam param = MultiModalEmbeddingParam.builder()
                .model(this.modelName)
                .apiKey(this.apiKey)
                .contents(contents)
                .parameter("enable_fusion", true)
                .parameter("dimension", dimension)
                .build();
        try {
            MultiModalEmbeddingResult generationResult = this.embedding.call(param);
            Embedding embedding = toEmbedding(generationResult.getOutput());
            int tokenCount = generationResult.getUsage().getInputTokens();
            return new EmbeddingResult(embedding, tokenCount);
        } catch (NoApiKeyException | UploadFileException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private List<MultiModalEmbeddingItemBase> toContents(EmbeddingInput input) {
        List<MultiModalEmbeddingItemBase> contents = new ArrayList<>();
        List<Content> inputContents = input.contents();
        for (Content inputContent : inputContents) {
            if (inputContent instanceof ImageContent imageContent) {
                contents.add(new MultiModalEmbeddingItemImage(toBase64ImageUrl(imageContent.image())));
            }
            if (inputContent instanceof TextContent textContent) {
                contents.add(new MultiModalEmbeddingItemText(textContent.text()));
            }
        }
        return contents;
    }


    public static class Builder {

        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Integer dimension;

        public Builder() {
            // This is public so it can be extended
            // By default with Lombok it becomes package private
        }

        public QwenMultiModalEmbeddingModel.Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public QwenMultiModalEmbeddingModel.Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public QwenMultiModalEmbeddingModel.Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public QwenMultiModalEmbeddingModel.Builder dimension(Integer dimension) {
            this.dimension = dimension;
            return this;
        }

        public QwenMultiModalEmbeddingModel build() {
            return new QwenMultiModalEmbeddingModel(baseUrl, apiKey, modelName, dimension);
        }
    }
}
