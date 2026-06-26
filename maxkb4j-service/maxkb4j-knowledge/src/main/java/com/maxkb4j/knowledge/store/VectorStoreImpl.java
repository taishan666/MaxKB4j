package com.maxkb4j.knowledge.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.util.BatchUtil;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.mapper.EmbeddingMapper;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.util.Tokenizer;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PostgreSQL pgvector implementation of VectorStore (MyBatis-Plus backend)
 * <p>与 {@link PgVectorEmbeddingStoreImpl} 互斥，由配置 {@code knowledge.store.vector.backend} 控制。</p>
 */
@Slf4j
@Component("vectorStore")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "knowledge.vector.type", havingValue = "mybatis")
public class VectorStoreImpl extends BaseStoreImpl {

    private final EmbeddingMapper embeddingMapper;
    private final KnowledgeModelService knowledgeModelService;
    /**
     * 用 {@link ObjectProvider} 延迟解析 paragraphService，避免与 ParagraphService 形成
     * 构造期循环依赖：paragraphService → problemService → problemParagraphService → compositeStore → vectorStore。
     * 实际只在 {@link #search(SearchRequest)} 中通过 {@link ObjectProvider#getObject()} 取真实 bean。
     */
    private final ObjectProvider<IParagraphService> paragraphServiceProvider;

    @Value("${vector.store.batch-size:10}")
    private int batchSize = 10;

    @Value("${vector.store.retry-times:3}")
    private int retryTimes = 3;

    @Value("${vector.store.retry-delay-ms:1000}")
    private int retryDelayMs = 1000;


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
        log.debug("Processing {} valid entities for embedding", validEntities.size());
        // Track successfully processed entities
        List<EmbeddingEntity> processedEntities = new CopyOnWriteArrayList<>();
        List<EmbeddingEntity> failedEntities = new CopyOnWriteArrayList<>();
        // Process in batches with configurable batch size
        BatchUtil.protectBach(validEntities, batchSize, batch -> {
            try {
                processBatchWithRetry(model, batch, processedEntities);
            } catch (Exception e) {
                log.error("Failed to process batch after retries: {}", e.getMessage(), e);
                failedEntities.addAll(batch);
            }
        });

        // Insert successfully processed entities into PostgreSQL
        if (!processedEntities.isEmpty()) {
            try {
                embeddingMapper.insert(processedEntities);
                log.debug("Inserted {} embedding entities into PostgreSQL", processedEntities.size());
            } catch (Exception e) {
                log.error("Failed to insert embeddings into PostgreSQL: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to insert embeddings", e);
            }
        }
        // Log failed entities for later processing
        if (!failedEntities.isEmpty()) {
            log.warn("Failed to process {} entities. They can be retried later.", failedEntities.size());
        }
    }

    /**
     * Process a batch with retry mechanism
     */
    private void processBatchWithRetry(EmbeddingModel model, List<EmbeddingEntity> batch,
                                       List<EmbeddingEntity> processedEntities) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                List<TextSegment> textSegments = batch.stream()
                        .map(e -> TextSegment.from(e.getContent()))
                        .toList();
                Response<List<Embedding>> res = model.embedAll(textSegments);
                List<Embedding> embeddings = res.content();

                for (int i = 0; i < batch.size(); i++) {
                    Embedding embedding = embeddings.get(i);
                    EmbeddingEntity embeddingEntity = batch.get(i);
                    embeddingEntity.setEmbedding(embedding.vectorAsList());
                    embeddingEntity.setContent(Tokenizer.segment(embeddingEntity.getContent()));
                    embeddingEntity.setDimension(model.dimension());
                }
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
        LambdaQueryWrapper<EmbeddingEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId, knowledgeId);
        queryWrapper.eq(EmbeddingEntity::getParagraphId, paragraphId);
        queryWrapper.eq(EmbeddingEntity::getSourceId, problemId);
        embeddingMapper.delete(queryWrapper);
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<EmbeddingEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId, knowledgeId);
        queryWrapper.eq(EmbeddingEntity::getSourceType, SourceType.PROBLEM);
        queryWrapper.in(EmbeddingEntity::getSourceId, problemIds);
        embeddingMapper.delete(queryWrapper);
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (paragraphIds == null || paragraphIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<EmbeddingEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(EmbeddingEntity::getParagraphId, paragraphIds);
        if (knowledgeId != null) {
            queryWrapper.eq(EmbeddingEntity::getKnowledgeId, knowledgeId);
        }
        embeddingMapper.delete(queryWrapper);
        log.debug("Deleted embeddings by paragraph IDs: {}", paragraphIds);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<EmbeddingEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(EmbeddingEntity::getDocumentId, documentIds);
        if (knowledgeId != null) {
            queryWrapper.eq(EmbeddingEntity::getKnowledgeId, knowledgeId);
        }
        embeddingMapper.delete(queryWrapper);
        log.debug("Deleted embeddings by document IDs: {}", documentIds);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        LambdaQueryWrapper<EmbeddingEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId, knowledgeId);
        embeddingMapper.delete(queryWrapper);
        log.debug("Deleted embeddings for knowledge ID: {}", knowledgeId);
    }

    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (shouldShortCircuit(request)) {
            return Collections.emptyList();
        }
        try {
            // Note: This assumes all knowledge bases in the request use the same embedding model
            EmbeddingModel embeddingModel = getEmbeddingModel(request.getKnowledgeIds().getFirst());
            if (embeddingModel == null) {
                log.warn("No embedding model found for knowledge: {}", request.getKnowledgeIds().getFirst());
                return Collections.emptyList();
            }
            List<String> excludeParagraphIds = resolveExcludeParagraphIds(request, paragraphServiceProvider.getObject());
            Response<Embedding> res = embeddingModel.embed(request.getQuery());
            float[] queryVector = res.content().vector();
            List<TextChunkVO> results = embeddingMapper.search(
                    request.getKnowledgeIds(),
                    request.getExcludeDocumentIds(),
                    excludeParagraphIds,
                    request.getMinScore(),
                    queryVector,
                    embeddingModel.dimension()
            );
            return dedupAndRank(results, request.getTopK());
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Vector search service error", e);
        }
    }

    /**
     * Get embedding model for a knowledge base
     */
    protected EmbeddingModel getEmbeddingModel(String knowledgeId) {
        return knowledgeModelService.getEmbeddingModel(knowledgeId);
    }
}
