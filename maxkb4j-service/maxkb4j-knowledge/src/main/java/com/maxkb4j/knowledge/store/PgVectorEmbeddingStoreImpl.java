package com.maxkb4j.knowledge.store;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.util.BatchUtil;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Langchain4j {@link PgVectorEmbeddingStore} 实现的向量后端。
 * <p>与 {@link VectorStoreImpl} 互斥，由 {@code knowledge.vector.type=pgvector} 启用。</p>
 */
@Slf4j
@Component("vectorStore")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "knowledge.vector.type", havingValue = "pgvector", matchIfMissing = true)
public class PgVectorEmbeddingStoreImpl extends BaseStoreImpl {

    /** 召回放大倍数：langchain4j 检索 topK*RECALL_MULTIPLIER 条后在内存里做 paragraphId 去重 */
    private static final int RECALL_MULTIPLIER = 10;
    @Value("${vector.store.batch-size:10}")
    private int batchSize = 10;
    @Value("${vector.store.retry-times:3}")
    private int retryTimes = 3;
    @Value("${vector.store.retry-delay-ms:1000}")
    private int retryDelayMs = 1000;

    private final KnowledgeModelService knowledgeModelService;
    private final DataSource dataSource;
    /** 延迟解析以避免与 ParagraphService 之间的构造期循环依赖，仅在 search() 中使用。 */
    private final ObjectProvider<IParagraphService> paragraphServiceProvider;

    /**
     * 按 embedding 维度缓存 PgVectorEmbeddingStore 实例。
     * <p>原先的 {@code public static HashMap} 既线程不安全，又因 {@code getOrDefault} 而从未真正缓存（每次新建）。
     * 这里改为实例字段 + {@link ConcurrentHashMap#computeIfAbsent} 保证原子初始化与缓存命中。</p>
     */
    private final Map<Integer, EmbeddingStore<TextSegment>> stores = new ConcurrentHashMap<>();


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
        // Filter valid entities
        List<EmbeddingEntity> validEntities = entities.stream()
                .filter(e -> e != null && StringUtils.isNotBlank(e.getContent()))
                .toList();
        if (validEntities.isEmpty()) {
            return;
        }
        EmbeddingStore<TextSegment> store = get(model.dimension());
        log.debug("Processing {} valid entities for embedding", validEntities.size());
        // Track successfully processed entities
        List<EmbeddingEntity> processedEntities = new CopyOnWriteArrayList<>();
        List<EmbeddingEntity> failedEntities = new CopyOnWriteArrayList<>();
        BatchUtil.protectBach(validEntities, batchSize, batch -> {
            try {
                processBatchWithRetry(model,store, batch, processedEntities);
            } catch (Exception e) {
                log.error("Failed to process batch after retries: {}", e.getMessage(), e);
                failedEntities.addAll(batch);
            }
        });
        // Log failed entities for later processing
        if (!failedEntities.isEmpty()) {
            log.warn("Failed to process {} entities. They can be retried later.", failedEntities.size());
        }
    }

    /**
     * Process a batch with retry mechanism
     */
    private void processBatchWithRetry(EmbeddingModel model,EmbeddingStore<TextSegment> store, List<EmbeddingEntity> batch,
                                       List<EmbeddingEntity> processedEntities) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                List<TextSegment> textSegments = batch.stream().map(entity -> {
                    Metadata metadata = new Metadata();
                    metadata.put("knowledgeId", entity.getKnowledgeId());
                    metadata.put("documentId", entity.getDocumentId());
                    metadata.put("paragraphId", entity.getParagraphId());
                    metadata.put("sourceId", entity.getSourceId());
                    metadata.put("sourceType", entity.getSourceType());
                    return TextSegment.from(entity.getContent().trim(), metadata);
                }).toList();
                Response<List<Embedding>> res = model.embedAll(textSegments);
                List<Embedding> embeddings = res.content();
                store.addAll(embeddings, textSegments);
                processedEntities.addAll(batch);
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("Batch processing attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < retryTimes) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (lastException != null) {
            log.error("All {} retry attempts failed for batch of size {}", retryTimes, batch.size());
            throw new RuntimeException("Batch processing failed after retries", lastException);
        }
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId)
                .and(metadataKey("paragraphId").isEqualTo(paragraphId))
                .and(metadataKey("sourceId").isEqualTo(problemId));
        removeAllStores(filter);
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId)
                .and(metadataKey("sourceType").isEqualTo(SourceType.PROBLEM))
                .and(metadataKey("sourceId").isIn(problemIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (paragraphIds == null || paragraphIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId)
                .and(metadataKey("paragraphId").isIn(paragraphIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId)
                .and(metadataKey("documentId").isIn(documentIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId);
        removeAllStores(filter);
    }

    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (shouldShortCircuit(request)) {
            return Collections.emptyList();
        }
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(request.getKnowledgeIds().getFirst());
        if (embeddingModel == null) {
            log.warn("No embedding model found for knowledge: {}", request.getKnowledgeIds().getFirst());
            return Collections.emptyList();
        }
        List<String> excludeParagraphIds = resolveExcludeParagraphIds(request, paragraphServiceProvider.getObject());
        Response<Embedding> res = embeddingModel.embed(request.getQuery().trim());
        Embedding queryEmbedding = res.content();
        EmbeddingStore<TextSegment> store = get(queryEmbedding.dimension());
        Filter filter = metadataKey("knowledgeId").isIn(request.getKnowledgeIds());
        if (CollectionUtils.isNotEmpty(request.getExcludeDocumentIds())) {
            filter = filter.and(metadataKey("documentId").isNotIn(request.getExcludeDocumentIds()));
        }
        if (CollectionUtils.isNotEmpty(excludeParagraphIds)) {
            filter = filter.and(metadataKey("paragraphId").isNotIn(excludeParagraphIds));
        }
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .filter(filter)
                .query(request.getQuery().trim())
                .queryEmbedding(queryEmbedding)
                .maxResults(request.getTopK() * RECALL_MULTIPLIER)
                .minScore(request.getMinScore())
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
        List<TextChunkVO> results = searchResult.matches().stream().map(match -> {
            TextSegment segment = match.embedded();
            return new TextChunkVO(segment.metadata().getString("paragraphId"), match.score());
        }).toList();
        return dedupAndRank(results, request.getTopK());
    }

    private void removeAllStores(Filter filter) {
        stores.forEach((dimension, store) -> store.removeAll(filter));
    }
}
