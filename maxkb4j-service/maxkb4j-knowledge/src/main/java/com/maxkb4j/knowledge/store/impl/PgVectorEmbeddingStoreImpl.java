package com.maxkb4j.knowledge.store.impl;

import com.maxkb4j.common.util.BatchUtil;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.service.impl.ParagraphServiceImpl;
import com.maxkb4j.knowledge.service.impl.ProblemParagraphServiceImpl;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
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
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Langchain4j {@link PgVectorEmbeddingStore} 实现的向量后端。
 */
@Slf4j
@Component("vectorStore")
@RequiredArgsConstructor
public class PgVectorEmbeddingStoreImpl extends BaseStoreImpl {

    /** 写入/检索 metadata 使用的字段名，与 {@link BaseStoreImpl} 的 filter 保持一致。 */
    private static final String META_KNOWLEDGE_ID = "knowledgeId";
    private static final String META_DOCUMENT_ID = "documentId";
    private static final String META_SOURCE_ID = "sourceId";
    private static final String META_SOURCE_TYPE = "sourceType";

    @Value("${vector.store.batch-size:10}")
    private int batchSize = 10;
    @Value("${vector.store.retry-times:3}")
    private int retryTimes = 3;
    @Value("${vector.store.retry-delay-ms:1000}")
    private int retryDelayMs = 1000;

    private final KnowledgeModelService knowledgeModelService;
    private final DataSource dataSource;
    /** 延迟解析以避免与 ParagraphService 之间的构造期循环依赖，仅在 search() 中使用。 */
    private final ObjectProvider<ParagraphServiceImpl> paragraphServiceProvider;
    private final ObjectProvider<ProblemParagraphServiceImpl> problemParagraphServiceProvider;

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
            log.warn("Failed to process {} entities. They can be retried later.", failedEntities.size());
        }
    }

    /**
     * 分批处理并在失败时按指数退避重试。
     */
    private void processBatchWithRetry(EmbeddingModel model, EmbeddingStore<TextSegment> store, List<EmbeddingEntity> batch) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                List<TextSegment> textSegments = batch.stream().map(this::toTextSegment).toList();
                Response<List<Embedding>> res = model.embedAll(textSegments);
                store.addAll(res.content(), textSegments);
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
        Embedding queryEmbedding = embeddingModel.embed(request.getQuery().trim()).content();
        EmbeddingStore<TextSegment> store = get(queryEmbedding.dimension());
        resolveExcludeParagraphIds(request, paragraphServiceProvider.getObject());

        List<TextChunkVO> results = new ArrayList<>();
        results.addAll(searchParagraphs(request, queryEmbedding, store, request.getExcludeParagraphIds()));
        results.addAll(searchProblemParagraphs(request, queryEmbedding, store));
        return dedupAndRank(results, request.getTopK());
    }

    /**
     * 段落路召回：直接按段落向量检索，命中结果即目标段落。
     */
    private List<TextChunkVO> searchParagraphs(SearchRequest request, Embedding queryEmbedding,
                                               EmbeddingStore<TextSegment> store, List<String> excludeParagraphIds) {
        EmbeddingSearchResult<TextSegment> searchResult =
                store.search(buildParagraphSearchRequest(request, queryEmbedding, excludeParagraphIds));
        return toTextChunkVOs(searchResult);
    }

    /**
     * 问题路召回：先按问题向量检索命中的 problemId，再经 problem_paragraph 映射表
     * 转换为其关联段落，分值沿用命中问题的相似度。
     */
    private List<TextChunkVO> searchProblemParagraphs(SearchRequest request, Embedding queryEmbedding,
                                                     EmbeddingStore<TextSegment> store) {
        EmbeddingSearchResult<TextSegment> problemResult =
                store.search(buildProblemSearchRequest(request, queryEmbedding));
        List<TextChunkVO> problemHits = toTextChunkVOs(problemResult);
        if (problemHits.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Double> problemScoreById = problemHits.stream()
                .collect(Collectors.toMap(TextChunkVO::getSourceId, TextChunkVO::getScore));
        List<ProblemParagraphEntity> problemParagraphs = problemParagraphServiceProvider.getObject()
                .getActivePPbyProblemIds(problemHits.stream().map(TextChunkVO::getSourceId).toList());
        List<TextChunkVO> results = new ArrayList<>(problemParagraphs.size());
        for (ProblemParagraphEntity problemParagraph : problemParagraphs) {
            results.add(new TextChunkVO(problemParagraph.getParagraphId(),
                    problemScoreById.get(problemParagraph.getProblemId())));
        }
        return results;
    }

    /**
     * 将检索命中统一转换为 {@link TextChunkVO}，并用 {@link #denormalizeScore} 把
     * langchain4j 归一化得分还原为余弦相似度。
     */
    private List<TextChunkVO> toTextChunkVOs(EmbeddingSearchResult<TextSegment> searchResult) {
        return searchResult.matches().stream().map(match -> {
            TextSegment segment = match.embedded();
            return new TextChunkVO(segment.metadata().getString(META_SOURCE_ID), denormalizeScore(match.score()));
        }).toList();
    }

    /**
     * 将 langchain4j 归一化到 {@code [0,1]} 的相似度还原为 {@code [-1,1]} 的余弦相似度，
     * 与 {@link BaseStoreImpl} 中 {@code (minScore + 1.0) / 2.0} 的归一化互为逆运算。
     */
    private static double denormalizeScore(double score) {
        return 2.0 * score - 1.0;
    }

    private void removeAllStores(Filter filter) {
        stores.forEach((dimension, store) -> store.removeAll(filter));
    }
}