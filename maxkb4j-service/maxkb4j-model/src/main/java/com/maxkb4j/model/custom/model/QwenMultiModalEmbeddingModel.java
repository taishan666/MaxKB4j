package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.embeddings.*;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.community.model.dashscope.spi.QwenEmbeddingModelBuilderFactory;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.*;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingInput;
import dev.langchain4j.model.embedding.request.EmbeddingParameter;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.request.EmbeddingRequestParameters;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;
import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.alibaba.dashscope.embeddings.TextEmbedding.Models.TEXT_EMBEDDING_V1;
import static com.alibaba.dashscope.embeddings.TextEmbedding.Models.TEXT_EMBEDDING_V2;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;


public class QwenMultiModalEmbeddingModel extends DimensionAwareEmbeddingModel {


    private final String apiKey;
    private final String modelName;
    private final MultiModalEmbedding embedding;
    private final QwenEmbeddingModel qwenEmbeddingModel;
    private final Consumer<MultiModalEmbeddingParam.MultiModalEmbeddingParamBuilder<?, ?>> multiModalEmbeddingParamCustomizer = (p) -> {};

    public QwenMultiModalEmbeddingModel(String baseUrl, String apiKey, String modelName, Integer dimension) {
        if (Utils.isNullOrBlank(apiKey)) {
            throw new IllegalArgumentException(
                    "DashScope api key must be defined. Reference: https://www.alibabacloud.com/help/en/model-studio/get-api-key");
        }
        this.modelName = Utils.isNullOrBlank(modelName) ? QwenModelName.TEXT_EMBEDDING_V3 : modelName;
        this.apiKey = apiKey;
        this.dimension = ensureDimension(this.modelName, dimension);
        this.embedding = Utils.isNullOrBlank(baseUrl) ? new MultiModalEmbedding() : new MultiModalEmbedding(baseUrl);
        this.qwenEmbeddingModel = new QwenEmbeddingModel(baseUrl, apiKey, modelName, dimension);
    }

    @Override
    public Set<EmbeddingParameter<?>> supportedParameters() {
        return Set.of(EmbeddingRequestParameters.INPUT_TYPE, EmbeddingRequestParameters.DIMENSIONS);
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return isMultimodal(modelName) ? Set.of(ContentType.TEXT, ContentType.IMAGE, ContentType.VIDEO) : Set.of(ContentType.TEXT);
    }

    private static boolean isMultimodal(String modelName) {
        return modelName != null && (modelName.contains("-vl-") || modelName.contains("-version-") || modelName.endsWith("-version"));
    }

    @Override
    public EmbeddingResponse doEmbed(EmbeddingRequest request) {
        boolean multimodal = isMultimodal(modelName);
        if (multimodal) {
            List<Embedding> embeddings = new ArrayList<>();
            int tokenCount = 0;
            for (EmbeddingInput input : request.inputs()) {
                List<MultiModalEmbeddingItemBase> contents = toContents(input);
                MultiModalEmbeddingParam.MultiModalEmbeddingParamBuilder<?, ?> builder = MultiModalEmbeddingParam.builder()
                        .apiKey(this.apiKey)
                        .model(this.modelName)
                        .contents(contents);
                try {
                    this.multiModalEmbeddingParamCustomizer.accept(builder);
                    MultiModalEmbeddingResult generationResult = this.embedding.call(builder.build());
                    Embedding embedding = toEmbedding(generationResult.getOutput());
                    embeddings.add(embedding);
                    tokenCount = tokenCount + generationResult.getUsage().getTotalUsage();
                } catch (NoApiKeyException | UploadFileException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return EmbeddingResponse.builder().modelName(this.modelName).embeddings(embeddings).tokenUsage(new TokenUsage(tokenCount)).build();
        }
        return qwenEmbeddingModel.doEmbed(request);
    }

    private static Embedding toEmbedding(MultiModalEmbeddingOutput output) {
        List<Float> vector = Optional.ofNullable(output)
                .map(MultiModalEmbeddingOutput::getEmbedding)
                .orElse(List.of())
                .stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());
        if (vector.isEmpty()) {
            throw new IllegalArgumentException("Multi-modal embedding response contains no embedding vector");
        }
        return Embedding.from(vector);
    }

    private List<MultiModalEmbeddingItemBase> toContents(EmbeddingInput input) {
        List<MultiModalEmbeddingItemBase> contents = new ArrayList<>();
        List<Content> inputContents = input.contents();
        for (Content inputContent : inputContents) {
            if (inputContent instanceof ImageContent imageContent) {
                contents.add(new MultiModalEmbeddingItemImage(imageContent.image().base64Data()));
            }
            if (inputContent instanceof VideoContent videoContent) {
                contents.add(new MultiModalEmbeddingItemImage(videoContent.video().base64Data()));
            }
            if (inputContent instanceof TextContent textContent) {
                contents.add(new MultiModalEmbeddingItemText(textContent.text()));
            }
        }
        return contents;

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

    public static QwenEmbeddingModel.QwenEmbeddingModelBuilder builder() {
        for (QwenEmbeddingModelBuilderFactory factory : loadFactories(QwenEmbeddingModelBuilderFactory.class)) {
            return factory.get();
        }
        return new QwenEmbeddingModel.QwenEmbeddingModelBuilder();
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
