package com.maxkb4j.model.custom.model;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.UnsupportedFeatureException;
import dev.langchain4j.internal.RetryUtils;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModelListenerUtils;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.embedding.request.EmbeddingInput;
import dev.langchain4j.model.embedding.request.EmbeddingInputType;
import dev.langchain4j.model.embedding.request.EmbeddingParameter;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.request.EmbeddingRequestParameters;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 参照 dev.langchain4j.model.google.genai.GoogleGenAiEmbeddingModel 实现，
 * 区别在于不依赖 com.google.genai.Client（Google GenAI SDK），
 * 而是通过 cn.hutool.http.HttpUtil 直接调用 Generative Language REST API：
 * <pre>
 *   POST {apiEndpoint}/v1beta/models/{model}:embedContent        单条/多模态输入
 *   POST {apiEndpoint}/v1beta/models/{model}:batchEmbedContents  纯文本批量输入
 * </pre>
 * 仅支持 API Key 鉴权（x-goog-api-key 请求头），不支持 Vertex AI 的 GoogleCredentials。
 */
public class QwenEmbeddingModel extends DimensionAwareEmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(QwenEmbeddingModel.class);

    private static final String DEFAULT_API_ENDPOINT = "https://generativelanguage.googleapis.com";
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    private final String apiKey;
    private final String apiEndpoint;
    private final int timeoutMillis;
    private final Map<String, String> customHeaders;
    private final String modelName;
    private final Integer outputDimensionality;
    private final TaskTypeEnum taskType;
    private final String titleMetadataKey;
    private final Integer maxSegmentsPerBatch;
    private final Integer maxRetries;
    private final boolean logRequests;
    private final boolean logResponses;
    private final List<EmbeddingModelListener> listeners;

    public QwenEmbeddingModel(Builder builder) {
        this.apiKey = ValidationUtils.ensureNotBlank(builder.apiKey, "apiKey");
        this.apiEndpoint = Utils.isNullOrBlank(builder.apiEndpoint)
                ? DEFAULT_API_ENDPOINT
                : trimTrailingSlash(builder.apiEndpoint);
        Duration timeout = Utils.getOrDefault(builder.timeout, DEFAULT_TIMEOUT);
        this.timeoutMillis = (int) timeout.toMillis();
        this.customHeaders = builder.customHeaders;
        this.modelName = ValidationUtils.ensureNotBlank(builder.modelName, "modelName");
        this.outputDimensionality = builder.outputDimensionality;
        this.taskType = builder.taskType;
        this.titleMetadataKey = Utils.getOrDefault(builder.titleMetadataKey, "title");
        this.maxRetries = Utils.getOrDefault(builder.maxRetries, 3);
        this.maxSegmentsPerBatch = ValidationUtils.ensureGreaterThanZero(
                Utils.getOrDefault(builder.maxSegmentsPerBatch, 100), "maxSegmentsPerBatch");
        this.logRequests = Utils.getOrDefault(builder.logRequests, false);
        this.logResponses = Utils.getOrDefault(builder.logResponses, false);
        this.listeners = Utils.copy(builder.listeners);
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * 模型名允许 "gemini-embedding-001"、"models/xxx"、"tunedModels/xxx" 三种写法，
     * 逐段 URL 编码后拼进 REST 路径。
     */
    private String embedUrl(String method) {
        String modelPath = Arrays.stream(modelName.split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        return apiEndpoint + "/v1beta/models/" + modelPath + ":" + method;
    }

    private JSONObject post(String url, JSONObject body) {
        HttpRequest request = HttpUtil.createRequest(Method.POST, url)
                .header(API_KEY_HEADER, apiKey)
                .header("Content-Type", "application/json")
                .timeout(timeoutMillis);
        if (customHeaders != null) {
            customHeaders.forEach(request::header);
        }
        request.body(body.toJSONString());
        if (logRequests) {
            log.info("Request:\n- model: {}\n- url: {}\n- body: {}", modelName, url, body.toJSONString());
        }
        try (HttpResponse response = request.execute()) {
            String responseBody = response.body();
            if (!response.isOk()) {
                throw new HttpException(response.getStatus(),
                        "Google GenAI embedding request failed: " + responseBody);
            }
            return JSONObject.parseObject(responseBody);
        }
    }

    private JSONObject callEmbedContent(JSONObject content, JSONObject config) {
        JSONObject body = new JSONObject();
        body.put("content", content);
        if (!config.isEmpty()) {
            body.put("config", config);
        }
        return post(embedUrl("embedContent"), body);
    }

    private JSONObject callBatchEmbedContents(List<String> texts, JSONObject config) {
        JSONArray requests = new JSONArray();
        for (String text : texts) {
            JSONObject part = new JSONObject();
            part.put("text", text);
            JSONArray parts = new JSONArray();
            parts.add(part);
            JSONObject content = new JSONObject();
            content.put("parts", parts);
            JSONObject perRequest = new JSONObject();
            perRequest.put("model", modelName.startsWith("models/") ? modelName : "models/" + modelName);
            perRequest.put("content", content);
            if (!config.isEmpty()) {
                perRequest.put("config", config);
            }
            requests.add(perRequest);
        }
        JSONObject body = new JSONObject();
        body.put("requests", requests);
        return post(embedUrl("batchEmbedContents"), body);
    }

    /**
     * proto3 JSON 序列化的字段名可能是 camelCase（tokenCount）或 snake_case（token_count），两者都兼容。
     */
    private static Integer tokenCountOf(JSONObject embedding) {
        JSONObject statistics = embedding.getJSONObject("statistics");
        if (statistics == null) {
            return null;
        }
        Integer count = statistics.getInteger("tokenCount");
        return count != null ? count : statistics.getInteger("token_count");
    }

    @Override
    public List<EmbeddingModelListener> listeners() {
        return listeners;
    }

    @Override
    public ModelProvider provider() {
        return ModelProvider.GOOGLE_GENAI;
    }

    @Override
    public Set<EmbeddingParameter<?>> supportedParameters() {
        return Set.of(EmbeddingRequestParameters.INPUT_TYPE, EmbeddingRequestParameters.DIMENSIONS);
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return isEmbedding2(modelName) ? Set.of(ContentType.TEXT, ContentType.IMAGE) : Set.of(ContentType.TEXT);
    }

    @Override
    public EmbeddingResponse doEmbed(EmbeddingRequest request) {
        EmbeddingInputType inputType = request.inputType();
        boolean embedding2 = isEmbedding2(modelName);
        String effectiveTaskType = embedding2 ? null : toSdkTaskType(inputType);
        Integer effectiveDimensions = Utils.getOrDefault(request.dimensions(), outputDimensionality);

        JSONObject config = new JSONObject();
        if (effectiveTaskType != null) {
            config.put("taskType", effectiveTaskType);
        }
        if (effectiveDimensions != null) {
            config.put("outputDimensionality", effectiveDimensions);
        }

        boolean multimodal = request.inputs().stream()
                .flatMap(input -> input.contentTypes().stream())
                .anyMatch(type -> type != ContentType.TEXT);

        List<Embedding> embeddings = new ArrayList<>();
        int tokenCount = 0;
        boolean tokenCountReported = false;
        if (multimodal) {
            for (EmbeddingInput input : request.inputs()) {
                JSONObject content = toContent(input, inputType);
                JSONObject response = RetryUtils.withRetryMappingExceptions(
                        () -> callEmbedContent(content, config), maxRetries);
                JSONObject embedding = response.getJSONObject("embedding");
                if (embedding == null) {
                    continue;
                }
                JSONArray values = embedding.getJSONArray("values");
                if (values != null) {
                    embeddings.add(Embedding.from(values.toJavaList(Float.class)));
                }
                Integer tokens = tokenCountOf(embedding);
                if (tokens != null) {
                    tokenCount += tokens;
                    tokenCountReported = true;
                }
            }
        } else {
            List<String> texts = request.inputs().stream()
                    .map(input -> embedding2 ? applyTaskInstruction(input.text(), inputType) : input.text())
                    .collect(Collectors.toList());
            for (int i = 0; i < texts.size(); i += maxSegmentsPerBatch) {
                List<String> batch = texts.subList(i, Math.min(i + maxSegmentsPerBatch, texts.size()));
                JSONObject response = RetryUtils.withRetryMappingExceptions(
                        () -> callBatchEmbedContents(batch, config), maxRetries);
                JSONArray responseEmbeddings = response.getJSONArray("embeddings");
                if (responseEmbeddings == null) {
                    continue;
                }
                for (int j = 0; j < responseEmbeddings.size(); j++) {
                    JSONObject embedding = responseEmbeddings.getJSONObject(j);
                    JSONArray values = embedding.getJSONArray("values");
                    if (values != null) {
                        embeddings.add(Embedding.from(values.toJavaList(Float.class)));
                    }
                    Integer tokens = tokenCountOf(embedding);
                    if (tokens != null) {
                        tokenCount += tokens;
                        tokenCountReported = true;
                    }
                }
            }
        }

        EmbeddingResponse.Builder responseBuilder = EmbeddingResponse.builder()
                .embeddings(embeddings)
                .modelName(modelName);
        if (tokenCountReported) {
            responseBuilder.tokenUsage(new TokenUsage(tokenCount));
        }
        return responseBuilder.build();
    }

    private static boolean isEmbedding2(String modelName) {
        return modelName != null && modelName.contains("embedding-2");
    }

    private JSONObject toContent(EmbeddingInput input, EmbeddingInputType inputType) {
        boolean textOnly = input.contents().stream().allMatch(content -> content instanceof TextContent);
        JSONArray parts = new JSONArray();
        for (dev.langchain4j.data.message.Content content : input.contents()) {
            if (content instanceof TextContent textContent) {
                String text = textOnly ? applyTaskInstruction(textContent.text(), inputType) : textContent.text();
                JSONObject part = new JSONObject();
                part.put("text", text);
                parts.add(part);
            } else if (content instanceof ImageContent imageContent) {
                Image image = imageContent.image();
                if (image.base64Data() == null) {
                    throw new UnsupportedFeatureException("Gemini requires base64 image data (a plain URL is not supported)");
                }
                // REST 接口的 inlineData.data 本身就是 base64 字符串，无需再解码
                JSONObject inlineData = new JSONObject();
                inlineData.put("mimeType", Utils.getOrDefault(image.mimeType(), "image/png"));
                inlineData.put("data", image.base64Data());
                JSONObject part = new JSONObject();
                part.put("inlineData", inlineData);
                parts.add(part);
            } else {
                throw new UnsupportedFeatureException("Unsupported content type: " + content.type());
            }
        }
        JSONObject contentJson = new JSONObject();
        contentJson.put("parts", parts);
        return contentJson;
    }

    private String toSdkTaskType(EmbeddingInputType inputType) {
        if (inputType == null) {
            return taskType != null ? taskType.getSdkTaskType() : null;
        }
        return switch (inputType) {
            case QUERY -> TaskTypeEnum.RETRIEVAL_QUERY.getSdkTaskType();
            case DOCUMENT -> TaskTypeEnum.RETRIEVAL_DOCUMENT.getSdkTaskType();
        };
    }

    private static String applyTaskInstruction(String text, EmbeddingInputType inputType) {
        if (inputType == null) {
            return text;
        }
        return switch (inputType) {
            case QUERY -> "task: search result | query: " + text;
            case DOCUMENT -> "title: none | text: " + text;
        };
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        return EmbeddingModelListenerUtils.withListeners(this, textSegments, () -> embedAllInternal(textSegments));
    }

    private Response<List<Embedding>> embedAllInternal(List<TextSegment> textSegments) {
        if (textSegments == null || textSegments.isEmpty()) {
            return Response.from(new ArrayList<>());
        }
        if (logRequests) {
            log.info("Request:\n- model: {}\n- texts: {}", modelName,
                    textSegments.stream().map(TextSegment::text).collect(Collectors.toList()));
        }

        Map<String, List<IndexedSegment>> grouped = new LinkedHashMap<>();
        for (int i = 0; i < textSegments.size(); i++) {
            TextSegment segment = textSegments.get(i);
            String title = null;
            if (TaskTypeEnum.RETRIEVAL_DOCUMENT.equals(taskType) && segment.metadata() != null) {
                title = segment.metadata().getString(titleMetadataKey);
            }
            grouped.computeIfAbsent(title, k -> new ArrayList<>()).add(new IndexedSegment(i, segment));
        }

        Embedding[] embeddingsArray = new Embedding[textSegments.size()];
        for (Map.Entry<String, List<IndexedSegment>> entry : grouped.entrySet()) {
            String title = entry.getKey();
            List<IndexedSegment> indexedSegments = entry.getValue();
            int size = indexedSegments.size();
            for (int i = 0; i < size; i += maxSegmentsPerBatch) {
                List<IndexedSegment> batch = indexedSegments.subList(i, Math.min(i + maxSegmentsPerBatch, size));
                List<String> texts = batch.stream().map(is -> is.segment.text()).collect(Collectors.toList());

                JSONObject config = new JSONObject();
                if (taskType != null) {
                    config.put("taskType", taskType.getSdkTaskType());
                }
                if (outputDimensionality != null) {
                    config.put("outputDimensionality", outputDimensionality);
                }
                if (title != null) {
                    config.put("title", title);
                }

                JSONObject response = RetryUtils.withRetryMappingExceptions(
                        () -> callBatchEmbedContents(texts, config), maxRetries);
                JSONArray responseEmbeddings = response.getJSONArray("embeddings");
                if (responseEmbeddings == null) {
                    continue;
                }
                for (int j = 0; j < batch.size() && j < responseEmbeddings.size(); j++) {
                    JSONArray values = responseEmbeddings.getJSONObject(j).getJSONArray("values");
                    if (values != null) {
                        embeddingsArray[batch.get(j).index] = Embedding.from(values.toJavaList(Float.class));
                    }
                }
            }
        }

        Response<List<Embedding>> response = Response.from(Arrays.asList(embeddingsArray));
        if (logResponses) {
            log.info("Response:\n- model: {}\n- response: {}", modelName, response);
        }
        return response;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public Integer knownDimension() {
        return outputDimensionality;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String modelName;
        private String apiKey;
        private String apiEndpoint;
        private Boolean logRequests;
        private Boolean logResponses;
        private Duration timeout;
        private Integer outputDimensionality;
        private TaskTypeEnum taskType;
        private String titleMetadataKey;
        private Map<String, String> customHeaders;
        private Integer maxSegmentsPerBatch = 100;
        private Integer maxRetries = 3;
        private List<EmbeddingModelListener> listeners;

        public Builder modelName(String modelName) {
            this.modelName = ValidationUtils.ensureNotBlank(modelName, "modelName");
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
            return this;
        }

        public Builder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public Builder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public Builder logRequestsAndResponses(Boolean logRequestsAndResponses) {
            this.logRequests = logRequestsAndResponses;
            this.logResponses = logRequestsAndResponses;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder outputDimensionality(Integer outputDimensionality) {
            this.outputDimensionality = outputDimensionality;
            return this;
        }

        public Builder taskType(TaskTypeEnum taskType) {
            this.taskType = taskType;
            return this;
        }

        public Builder titleMetadataKey(String titleMetadataKey) {
            this.titleMetadataKey = titleMetadataKey;
            return this;
        }

        public Builder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public Builder maxSegmentsPerBatch(Integer maxSegmentsPerBatch) {
            this.maxSegmentsPerBatch = maxSegmentsPerBatch;
            return this;
        }

        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder listeners(List<EmbeddingModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public QwenEmbeddingModel build() {
            return new QwenEmbeddingModel(this);
        }
    }

    public enum TaskTypeEnum {
        TASK_TYPE_UNSPECIFIED("TASK_TYPE_UNSPECIFIED"),
        RETRIEVAL_QUERY("RETRIEVAL_QUERY"),
        RETRIEVAL_DOCUMENT("RETRIEVAL_DOCUMENT"),
        SEMANTIC_SIMILARITY("SEMANTIC_SIMILARITY"),
        CLASSIFICATION("CLASSIFICATION"),
        CLUSTERING("CLUSTERING"),
        QUESTION_ANSWERING("QUESTION_ANSWERING"),
        FACT_VERIFICATION("FACT_VERIFICATION"),
        CODE_RETRIEVAL_QUERY("CODE_RETRIEVAL_QUERY");

        private final String sdkTaskType;

        TaskTypeEnum(String sdkTaskType) {
            this.sdkTaskType = sdkTaskType;
        }

        public String getSdkTaskType() {
            return sdkTaskType;
        }
    }

    private static class IndexedSegment {
        final int index;
        final TextSegment segment;

        IndexedSegment(int index, TextSegment segment) {
            this.index = index;
            this.segment = segment;
        }
    }
}
