package com.maxkb4j.model.custom.model;

import com.volcengine.ark.runtime.model.multimodalembeddings.MultimodalEmbedding;
import com.volcengine.ark.runtime.model.multimodalembeddings.MultimodalEmbeddingInput;
import com.volcengine.ark.runtime.model.multimodalembeddings.MultimodalEmbeddingRequest;
import com.volcengine.ark.runtime.model.multimodalembeddings.MultimodalEmbeddingResult;
import com.volcengine.ark.runtime.service.ArkService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingInput;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OpenAiMultiModalEmbeddingModel extends AbstractMultiModalEmbeddingModel {

    private final ArkService service;
    private final OpenAiEmbeddingModel embeddingModel;

    public OpenAiMultiModalEmbeddingModel(Builder builder) {
        super(builder.modelName);
        if (Utils.isNullOrBlank(builder.apiKey)) {
            throw new IllegalArgumentException("OpenAi api key must be defined.");
        }
        super.dimension = builder.dimensions;
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        this.service = ArkService.builder().dispatcher(new Dispatcher()).connectionPool(connectionPool).apiKey(builder.apiKey).build();
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .httpClientBuilder(builder.httpClientBuilder)
                .baseUrl(builder.baseUrl)
                .apiKey(builder.apiKey)
                .modelName(builder.modelName)
                .dimensions(builder.dimensions)
                .build();
    }

    private static Embedding toEmbedding(MultimodalEmbedding multimodalEmbedding) {
        List<Float> vector = Optional.ofNullable(multimodalEmbedding)
                .map(MultimodalEmbedding::getEmbedding)
                .orElse(List.of())
                .stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());
        if (vector.isEmpty()) {
            throw new IllegalArgumentException("Multi-modal embedding response contains no embedding vector");
        }
        return Embedding.from(vector);
    }

    public static OpenAiMultiModalEmbeddingModel.Builder builder() {
        return new OpenAiMultiModalEmbeddingModel.Builder();
    }

    @Override
    protected DimensionAwareEmbeddingModel textEmbeddingModel() {
        return embeddingModel;
    }

    @Override
    protected EmbeddingResult embedMultimodal(EmbeddingInput input) {
        List<MultimodalEmbeddingInput> inputs = toContents(input);
        MultimodalEmbeddingRequest multiModalEmbeddingRequest = MultimodalEmbeddingRequest.builder()
                .model(this.modelName)
                .dimensions(this.dimension)
                .input(inputs)
                .build();
        MultimodalEmbeddingResult res = service.createMultiModalEmbeddings(multiModalEmbeddingRequest);
        Embedding embedding = toEmbedding(res.getData());
        int tokenCount = (int) (res.getUsage().getTotalTokens());
        return new EmbeddingResult(embedding, tokenCount);
    }

    private List<MultimodalEmbeddingInput> toContents(EmbeddingInput input) {
        List<MultimodalEmbeddingInput> inputs = new ArrayList<>();
        List<Content> inputContents = input.contents();
        for (Content inputContent : inputContents) {
            if (inputContent instanceof ImageContent imageContent) {
                inputs.add(MultimodalEmbeddingInput.builder().type("image_url").imageUrl(
                        new MultimodalEmbeddingInput.MultiModalEmbeddingContentPartImageURL(
                                toBase64ImageUrl(imageContent.image())
                        )
                ).build());
            }
            if (inputContent instanceof TextContent textContent) {
                inputs.add(MultimodalEmbeddingInput.builder().type("text").text(textContent.text()).build());
            }
        }
        return inputs;
    }



    public static class Builder {

        private HttpClientBuilder httpClientBuilder;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Integer dimensions;

        public Builder() {
            // This is public so it can be extended
            // By default with Lombok it becomes package private
        }

        public OpenAiMultiModalEmbeddingModel.Builder httpClientBuilder(HttpClientBuilder httpClientBuilder) {
            this.httpClientBuilder = httpClientBuilder;
            return this;
        }

        public OpenAiMultiModalEmbeddingModel.Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiMultiModalEmbeddingModel.Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiMultiModalEmbeddingModel.Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiMultiModalEmbeddingModel.Builder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public OpenAiMultiModalEmbeddingModel build() {
            return new OpenAiMultiModalEmbeddingModel(this);
        }
    }
}
