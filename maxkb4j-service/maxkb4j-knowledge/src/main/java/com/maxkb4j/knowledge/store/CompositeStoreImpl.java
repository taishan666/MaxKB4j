package com.maxkb4j.knowledge.store;

import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Composite vector store that combines multiple stores for unified access
 * Supports dual-write to both PostgreSQL and MongoDB for data consistency
 */
@Slf4j
@Component("compositeStore")
@RequiredArgsConstructor
public class CompositeStoreImpl implements IDataStore {

    private final VectorStoreImpl vectorStore;
    private final FullTextStoreImpl fullTextStore;


    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        log.debug("Upserting {} entities to both PostgreSQL and MongoDB", entities.size());
        try {
            // Dual-write: insert to PostgreSQL (with vector embeddings)
            vectorStore.upsert(model, entities);
            // Insert to MongoDB (for full-text search)
            fullTextStore.upsert(model, entities);
            log.debug("Successfully upserted {} entities to both stores", entities.size());
        } catch (Exception e) {
            log.error("Failed to upsert entities: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upsert entities to vector stores", e);
        }
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        try {
            vectorStore.deleteByProblemIdAndParagraphId(knowledgeId, problemId, paragraphId);
            fullTextStore.deleteByProblemIdAndParagraphId(knowledgeId, problemId, paragraphId);
        } catch (Exception e) {
            log.error("Failed to delete by paragraph IDs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete from vector stores", e);
        }
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        try {
            vectorStore.deleteProblemByIds(knowledgeId, problemIds);
            fullTextStore.deleteProblemByIds(knowledgeId, problemIds);
        } catch (Exception e) {
            log.error("Failed to delete by paragraph IDs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete from vector stores", e);
        }
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        log.debug("Deleting embeddings by paragraph IDs from both stores");
        try {
            vectorStore.deleteByParagraphIds(knowledgeId, paragraphIds);
            fullTextStore.deleteByParagraphIds(knowledgeId, paragraphIds);
        } catch (Exception e) {
            log.error("Failed to delete by paragraph IDs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete from vector stores", e);
        }
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        log.debug("Deleting embeddings by document IDs from both stores");
        try {
            vectorStore.deleteByDocumentIds(knowledgeId, documentIds);
            fullTextStore.deleteByDocumentIds(knowledgeId, documentIds);
        } catch (Exception e) {
            log.error("Failed to delete by document IDs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete from vector stores", e);
        }
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        log.debug("Deleting embeddings for knowledge ID from both stores");
        try {
            vectorStore.deleteByKnowledgeId(knowledgeId);
            fullTextStore.deleteByKnowledgeId(knowledgeId);
        } catch (Exception e) {
            log.error("Failed to delete by knowledge ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete from vector stores", e);
        }
    }


    /**
     * Perform hybrid search combining vector and full-text search
     */
    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        Map<String, Double> map = new LinkedHashMap<>();
        List<TextChunkVO> results = new ArrayList<>();
        List<CompletableFuture<List<TextChunkVO>>> futureList = new ArrayList<>();
        futureList.add(CompletableFuture.supplyAsync(()->vectorStore.search(request)));
        futureList.add(CompletableFuture.supplyAsync(()->fullTextStore.search(request)));
        List<TextChunkVO> retrieveResults = futureList.stream().flatMap(future-> future.join().stream()).toList();
        //融合排序
        for (TextChunkVO result : retrieveResults) {
            if (map.containsKey(result.getParagraphId())) {
                if (map.get(result.getParagraphId()) < result.getScore()) {
                    map.put(result.getParagraphId(), result.getScore());
                }
            } else {
                map.put(result.getParagraphId(), result.getScore());
            }
        }
        map.forEach((key, value) -> results.add(new TextChunkVO(key, value)));
        results.sort(Comparator.comparing(TextChunkVO::getScore).reversed());
        int endIndex = Math.min(request.getTopK(), results.size());
        return results.subList(0, endIndex);
    }


}