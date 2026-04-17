package com.maxkb4j.knowledge.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.util.BatchUtil;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.mapper.EmbeddingMapper;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * PostgreSQL pgvector implementation of VectorStore
 */
@Slf4j
@Component("vectorStore")
@RequiredArgsConstructor
public class VectorStoreImpl implements IDataStore {

    private final EmbeddingMapper embeddingMapper;
    private final KnowledgeModelService knowledgeModelService;

    @Value("${vector.store.batch-size:100}")
    private int batchSize = 100;

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
                .collect(Collectors.toList());

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
                processBatchWithRetry(model, batch, processedEntities, failedEntities);
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
            // Optionally, store failed entities for later retry
        }
    }



    /**
     * Process a batch with retry mechanism
     */
    private void processBatchWithRetry(EmbeddingModel model, List<EmbeddingEntity> batch,
                                       List<EmbeddingEntity> processedEntities,
                                       List<EmbeddingEntity> failedEntities) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                // Create text segments for embedding
                List<TextSegment> textSegments = batch.stream()
                        .map(e -> TextSegment.from(e.getContent()))
                        .toList();
                // Generate embeddings for the batch
                Response<List<Embedding>> res = model.embedAll(textSegments);
                List<Embedding> embeddings = res.content();

                // Update entities with embeddings
                for (int i = 0; i < batch.size(); i++) {
                    Embedding embedding = embeddings.get(i);
                    EmbeddingEntity embeddingEntity = batch.get(i);
                    embeddingEntity.setEmbedding(embedding.vectorAsList());
                    embeddingEntity.setContent(Tokenizer.segment(embeddingEntity.getContent()));
                    embeddingEntity.setDimension(model.dimension());
                }
                // Successfully processed
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

        // All retries failed
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
    public void updateActiveStatus(String knowledgeId, String paragraphId, boolean isActive) {
        LambdaUpdateWrapper<EmbeddingEntity> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.set(EmbeddingEntity::getIsActive, isActive)
                .eq(EmbeddingEntity::getKnowledgeId, knowledgeId)
                .eq(EmbeddingEntity::getParagraphId, paragraphId);
        embeddingMapper.update(updateWrapper);
        log.debug("Updated active status for paragraph: {} to {}", paragraphId, isActive);
    }

    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (request.getKnowledgeIds() == null || request.getKnowledgeIds().isEmpty()) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(request.getQuery())) {
            return Collections.emptyList();
        }
        try {
            // Note: This assumes all knowledge bases in the request use the same embedding model
            EmbeddingModel embeddingModel = getEmbeddingModel(request.getKnowledgeIds().get(0));
            if (embeddingModel == null) {
                log.warn("No embedding model found for knowledge: {}", request.getKnowledgeIds().get(0));
                return Collections.emptyList();
            }
            // Generate embedding for query
            Response<Embedding> res = embeddingModel.embed(request.getQuery());
            float[] queryVector = res.content().vector();
            // Perform vector search
            List<TextChunkVO> results = embeddingMapper.search(
                    request.getKnowledgeIds(),
                    request.getExcludeDocumentIds(),
                    request.getExcludeParagraphIds(),
                    request.getTopK(),
                    request.getMinScore(),
                    queryVector,
                    embeddingModel.dimension()
            );
            //todo
            return results;
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