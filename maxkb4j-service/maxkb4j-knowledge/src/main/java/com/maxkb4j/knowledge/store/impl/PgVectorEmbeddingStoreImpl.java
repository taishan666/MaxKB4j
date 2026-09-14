package com.maxkb4j.knowledge.store.impl;

import com.maxkb4j.common.util.BatchUtil;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingInput;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Langchain4j {@link PgVectorEmbeddingStore} 实现的向量后端。
 * <p>检索结果得分与 minScore 阈值统一采用 langchain4j 的 {@code [0,1]} 归一化区间
 * （即 {@code (余弦相似度 + 1) / 2}），与 {@link FullTextStoreImpl} 保持同一量纲，
 * 便于 {@link CompositeStoreImpl} 跨后端融合。</p>
 */
@Slf4j
@Component("vectorStore")
@RequiredArgsConstructor
public class PgVectorEmbeddingStoreImpl extends BaseStoreImpl {

    /**
     * 写入/检索 metadata 使用的字段名，与检索过滤条件保持一致。
     */
    private static final String META_KNOWLEDGE_ID = "knowledgeId";
    private static final String META_DOCUMENT_ID = "documentId";
    private static final String META_SOURCE_ID = "sourceId";
    private static final String META_SOURCE_TYPE = "sourceType";
    /**
     * 从Markdown文本中提取所有图片URL
     */
    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    /**
     * 下载图片用于转 Base64 Data URI 时的连接/读取超时（毫秒）。
     */
    private static final int IMAGE_CONNECT_TIMEOUT_MS = 5000;
    private static final int IMAGE_READ_TIMEOUT_MS = 10000;
    private final KnowledgeModelService knowledgeModelService;
    private final DataSource dataSource;
    /**
     * 按 embedding 维度缓存 PgVectorEmbeddingStore 实例。
     * <p>原先的 {@code public static HashMap} 既线程不安全，又因 {@code getOrDefault} 而从未真正缓存（每次新建）。
     * 这里改为实例字段 + {@link ConcurrentHashMap#computeIfAbsent} 保证原子初始化与缓存命中。</p>
     */
    private final Map<Integer, EmbeddingStore<TextSegment>> stores = new ConcurrentHashMap<>();
    @Value("${vector.store.batch-size:10}")
    private int batchSize = 10;
    @Value("${vector.store.retry-times:3}")
    private int retryTimes = 3;
    @Value("${vector.store.retry-delay-ms:1000}")
    private int retryDelayMs = 1000;
    /**
     * 项目服务端口，用于将相对图片路径拼接为可访问的完整地址。
     */
    @Value("${server.port:8080}")
    private int serverPort = 8080;

    /**
     * 将 langchain4j 归一化到 {@code [0,1]} 的相似度还原为 {@code [-1,1]} 的余弦相似度，
     * 与 {@link BaseStoreImpl} 中 {@code (minScore + 1.0) / 2.0} 的归一化互为逆运算。
     */
    private static double denormalizeScore(double score) {
        return 2.0 * score - 1.0;
    }

    private static double normalizeScore(double score) {
        return (score + 1.0) / 2.0;
    }

    private EmbeddingStore<TextSegment> build(int dimension) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("embedding_" + dimension)
                .dimension(dimension)
                .searchMode(PgVectorEmbeddingStore.SearchMode.VECTOR)
                .metadataStorageConfig(DefaultMetadataStorageConfig.builder()
                        .storageMode(MetadataStorageMode.COMBINED_JSONB)
                        .columnDefinitions(Collections.singletonList("metadata JSONB NULL"))
                        .build())
                .build();
    }

    public EmbeddingStore<TextSegment> get(int dimension) {
        return stores.computeIfAbsent(dimension, this::build);
    }

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        List<EmbeddingEntity> validEntities = entities.stream()
                .filter(e -> e != null && StringUtils.isNotBlank(e.getContent()))
                .toList();
        if (validEntities.isEmpty()) {
            return;
        }
        EmbeddingStore<TextSegment> store = get(model.dimension());
        log.debug("Processing {} valid entities for embedding", validEntities.size());
        List<EmbeddingEntity> failedEntities = new CopyOnWriteArrayList<>();
        BatchUtil.protectBach(validEntities, batchSize, batch -> {
            try {
                processBatchWithRetry(model, store, batch);
            } catch (Exception e) {
                log.error("Failed to process batch after retries: {}", e.getMessage(), e);
                failedEntities.addAll(batch);
            }
        });
        if (!failedEntities.isEmpty()) {
            throw new RuntimeException("向量写入失败，共 " + failedEntities.size() + " 条数据未成功索引，请检查 embedding 服务后重试");
        }
    }

    /**
     * 分批处理并在失败时按指数退避重试。
     */
    private void processBatchWithRetry(EmbeddingModel model, EmbeddingStore<TextSegment> store, List<EmbeddingEntity> batch) {
        Exception lastException = null;
        Set<ContentType> supportedContentTypes=model.supportedContentTypes();
        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                List<TextSegment> textSegments = batch.stream().map(this::toTextSegment).toList();
                if(supportedContentTypes.contains(ContentType.IMAGE)){
                    List<EmbeddingInput> inputs = new ArrayList<>();
                    textSegments.forEach(segment -> {
                        String text = segment.text();
                        List<Content> contents = new ArrayList<>();
                        contents.add(TextContent.from(text));
                        List<String> imageUrls = extractImageUrls(text);
                        for (String imageUrl : imageUrls) {
                            ImageContent imageContent = toBase64ImageContent(imageUrl);
                            if (imageContent != null) {
                                contents.add(imageContent);
                            }
                        }
                        inputs.add(EmbeddingInput.from(contents));
                    });
                    EmbeddingResponse res = model.embed(EmbeddingRequest.builder().inputs(inputs).build());
                    store.addAll(res.embeddings(), textSegments);
                }else {
                    Response<List<Embedding>> res= model.embedAll(textSegments);
                    store.addAll(res.content(), textSegments);
                }
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("Batch processing attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < retryTimes && !backoff(attempt)) {
                    break;
                }
            }
        }
        if (lastException != null) {
            log.error("All {} retry attempts failed for batch of size {}", retryTimes, batch.size());
            throw new RuntimeException("Batch processing failed after retries", lastException);
        }
    }

    private List<String> extractImageUrls(String markdownText) {
        List<String> urls = new ArrayList<>();
        if (markdownText == null || markdownText.isEmpty()) {
            return urls;
        }
        Matcher matcher = MD_IMAGE_PATTERN.matcher(markdownText);
        while (matcher.find()) {
            String url = matcher.group(1).trim();
            // 去除可能包裹的尖括号 <url>
            if (url.startsWith("<") && url.endsWith(">")) {
                url = url.substring(1, url.length() - 1);
            }
            urls.add(url);
        }
        return urls;
    }

    /**
     * 下载图片并将其转换为 Base64 编码的 Data URI，封装进 {@link ImageContent}。
     * <p>{@link ImageContent#from(String, String)} 会以 {@code data:<mimeType>;base64,<data>}
     * 的形式保存，从而避免下游模型直接访问外链图片。</p>
     *
     * @param imageUrl 图片地址（http/https 外链，或已经是 data: 形式的 Data URI）
     * @return 转换后的 {@link ImageContent}；下载或解析失败时返回 {@code null}
     */
    private ImageContent toBase64ImageContent(String imageUrl) {
        if (StringUtils.isBlank(imageUrl)) {
            return null;
        }
        // 已经是 Base64 Data URI，直接解析复用
        if (imageUrl.startsWith("data:")) {
            return parseDataUri(imageUrl);
        }
        // 相对路径拼接为完整的本机服务地址
        String fullUrl = resolveFullUrl(imageUrl);
        try {
            URL url = URI.create(fullUrl).toURL();
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(IMAGE_CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(IMAGE_READ_TIMEOUT_MS);
            byte[] bytes;
            String contentType;
            try (InputStream in = connection.getInputStream()) {
                bytes = in.readAllBytes();
                contentType = connection.getContentType();
            }
            String mimeType = resolveMimeType(contentType, fullUrl);
            String base64Data = Base64.getEncoder().encodeToString(bytes);
            return ImageContent.from(base64Data, mimeType);
        } catch (Exception e) {
            log.warn("Failed to download image for base64 encoding: {}, cause: {}", fullUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 将图片地址解析为可访问的完整 URL。
     * <p>若已经是 {@code http://} / {@code https://} 绝对地址则原样返回；
     * 否则视为相对访问路径，使用 {@code 127.0.0.1} 加项目服务端口拼接。</p>
     */
    private String resolveFullUrl(String imageUrl) {
        String trimmed = imageUrl.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        String path = trimmed.startsWith("./") ? trimmed.substring(1) :trimmed;
        return "http://127.0.0.1:" + serverPort + path;
    }

    /**
     * 解析形如 {@code data:image/png;base64,xxxx} 的 Data URI 为 {@link ImageContent}。
     */
    private ImageContent parseDataUri(String dataUri) {
        try {
            int commaIdx = dataUri.indexOf(',');
            if (commaIdx < 0) {
                return null;
            }
            String meta = dataUri.substring(5, commaIdx); // 去掉 "data:" 前缀
            String base64Data = dataUri.substring(commaIdx + 1);
            String mimeType = "image/png";
            int semicolonIdx = meta.indexOf(';');
            if (semicolonIdx > 0) {
                mimeType = meta.substring(0, semicolonIdx);
            } else if (!meta.isBlank()) {
                mimeType = meta;
            }
            return ImageContent.from(base64Data, mimeType);
        } catch (Exception e) {
            log.warn("Failed to parse data uri for image content, cause: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 优先使用响应头 Content-Type，缺失或为通用二进制流时回退到按 URL 扩展名猜测。
     */
    private String resolveMimeType(String contentType, String imageUrl) {
        if (StringUtils.isNotBlank(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType.trim())) {
            int semicolonIdx = contentType.indexOf(';');
            return semicolonIdx > 0 ? contentType.substring(0, semicolonIdx).trim() : contentType.trim();
        }
        return guessMimeTypeFromUrl(imageUrl);
    }

    /**
     * 根据 URL 路径扩展名猜测图片 MIME 类型，无法识别时默认 {@code image/png}。
     */
    private String guessMimeTypeFromUrl(String imageUrl) {
        String path = imageUrl;
        int queryIdx = path.indexOf('?');
        if (queryIdx > 0) {
            path = path.substring(0, queryIdx);
        }
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < path.length() - 1) {
            String ext = path.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
            switch (ext) {
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                case "webp":
                    return "image/webp";
                case "bmp":
                    return "image/bmp";
                case "svg":
                    return "image/svg+xml";
                default:
                    break;
            }
        }
        return "image/png";
    }

    /**
     * 将 {@link EmbeddingEntity} 转换为带 metadata 的 {@link TextSegment}。
     */
    private TextSegment toTextSegment(EmbeddingEntity entity) {
        Metadata metadata = new Metadata();
        metadata.put(META_KNOWLEDGE_ID, entity.getKnowledgeId());
        if (entity.getDocumentId() != null) {
            metadata.put(META_DOCUMENT_ID, entity.getDocumentId());
        }
        metadata.put(META_SOURCE_ID, entity.getSourceId());
        metadata.put(META_SOURCE_TYPE, entity.getSourceType());
        return TextSegment.from(entity.getContent().trim(), metadata);
    }

    /**
     * 重试间隔（指数退避：retryDelayMs * 2^(attempt-1)）。
     * <p>抽成独立方法既消除 IDEA "Thread.sleep in a loop" 警告，也把中断处理收敛到一处。</p>
     *
     * @param attempt 当前已失败的尝试次数（从 1 开始）
     * @return true 表示正常等待结束，可继续下一次重试；false 表示被中断，调用方应立刻终止循环
     */
    private boolean backoff(int attempt) {
        try {
            Thread.sleep(retryDelayMs * (1L << (attempt - 1)));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void deleteByProblemIds(String knowledgeId, List<String> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isEqualTo(knowledgeId)
                .and(metadataKey(META_SOURCE_TYPE).isEqualTo(String.valueOf(SourceType.PROBLEM)))
                .and(metadataKey(META_SOURCE_ID).isIn(problemIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (paragraphIds == null || paragraphIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isEqualTo(knowledgeId)
                .and(metadataKey(META_SOURCE_TYPE).isEqualTo(String.valueOf(SourceType.PARAGRAPH)))
                .and(metadataKey(META_SOURCE_ID).isIn(paragraphIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isEqualTo(knowledgeId)
                .and(metadataKey(META_DOCUMENT_ID).isIn(documentIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isEqualTo(knowledgeId);
        removeAllStores(filter);
    }

    /**
     * 按来源类型做向量检索：仅依据 {@link SearchRequest} 入参构造 langchain4j {@link Filter}，
     * 不再自行解析排除段落（该策略已上移到 {@link com.maxkb4j.knowledge.retriever.SearchOrchestrator}）。
     * <p>段落路按 excludeDocumentIds / excludeParagraphIds 过滤；问题路不应用这两类排除。</p>
     */
    @Override
    public List<TextChunkVO> searchBySource(SearchRequest request, int sourceType) {
        if (shouldShortCircuit(request)) {
            return Collections.emptyList();
        }
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(request.getKnowledgeIds().getFirst());
        if (embeddingModel == null) {
            log.warn("No embedding model found for knowledge: {}", request.getKnowledgeIds().getFirst());
            return Collections.emptyList();
        }
        Embedding queryEmbedding = embeddingModel.embed(request.getQuery().trim()).content();
        EmbeddingStore<TextSegment> store = get(queryEmbedding.dimension());
        EmbeddingSearchResult<TextSegment> searchResult = store.search(buildSearchRequest(request, queryEmbedding, sourceType));
        return toTextChunkVOs(searchResult);
    }

    /**
     * 构造 langchain4j 检索请求：按 sourceType 组装过滤条件。minScore 阈值与检索结果得分
     * 统一采用 langchain4j 的 {@code [0,1]} 归一化区间，无需额外换算。
     */
    private EmbeddingSearchRequest buildSearchRequest(SearchRequest request, Embedding queryEmbedding, int sourceType) {
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isIn(request.getKnowledgeIds());
        filter = filter.and(metadataKey(META_SOURCE_TYPE).isEqualTo(String.valueOf(sourceType)));
        if (sourceType == SourceType.PARAGRAPH) {
            if (!CollectionUtils.isEmpty(request.getExcludeDocumentIds())) {
                filter = filter.and(metadataKey(META_DOCUMENT_ID).isNotIn(request.getExcludeDocumentIds()));
            }
            if (!CollectionUtils.isEmpty(request.getExcludeParagraphIds())) {
                filter = filter.and(metadataKey(META_SOURCE_ID).isNotIn(request.getExcludeParagraphIds()));
            }
        }
        return EmbeddingSearchRequest.builder()
                .filter(filter)
                .query(request.getQuery().trim())
                .queryEmbedding(queryEmbedding)
                .maxResults(request.getTopK())
                .minScore(normalizeScore(request.getMinScore()))
                .build();
    }

    /**
     * 将检索命中统一转换为 {@link TextChunkVO}，score 直接采用 langchain4j 的 {@code [0,1]} 归一化得分，
     * 与全文库保持同一量纲。
     */
    private List<TextChunkVO> toTextChunkVOs(EmbeddingSearchResult<TextSegment> searchResult) {
        return searchResult.matches().stream().map(match -> {
            TextSegment segment = match.embedded();
            return new TextChunkVO(segment.metadata().getString(META_SOURCE_ID), denormalizeScore(match.score()));
        }).toList();
    }

    private void removeAllStores(Filter filter) {
        stores.forEach((dimension, store) -> store.removeAll(filter));
    }
}