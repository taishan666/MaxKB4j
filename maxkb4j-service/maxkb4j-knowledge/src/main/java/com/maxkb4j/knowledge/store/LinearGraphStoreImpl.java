package com.maxkb4j.knowledge.store;

import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.linearrag.LinearRagGraphService;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * LinearRAG-based graph retrieval store implementation.
 *
 * Implements the LinearRAG paper's approach:
 * - Tri-Graph (Entity-Sentence-Paragraph) structure
 * - Zero-Token graph construction (using HanLP NER + TextRank + jieba, no LLM calls)
 * - Retrieval pipeline: Seed Entities → BFS Diffusion → DPR + Entity Bonus → PPR
 * - Linear complexity graph construction
 */
@Slf4j
@Component("linearGraphStore")
@RequiredArgsConstructor
public class LinearGraphStoreImpl implements IDataStore {

    private final LinearRagGraphService graphService;

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        // LinearRAG graph data is managed through LinearRagGraphService
        // Entity extraction is triggered via GraphExtractionEvent
        log.debug("LinearGraphStore upsert - entities managed via LinearRagGraphService");
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        graphService.deleteByParagraphIds(knowledgeId, List.of(paragraphId));
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        // Graph store doesn't store problem-based data
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        graphService.deleteByParagraphIds(knowledgeId, paragraphIds);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        graphService.deleteByDocumentIds(knowledgeId, documentIds);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        graphService.deleteByKnowledgeId(knowledgeId);
    }

    @Override
    public void updateActiveStatus(String knowledgeId, String paragraphId, boolean isActive) {
        // No-op: graph is rebuilt on each retrieval, no cache to invalidate
    }

    /**
     * Execute LinearRAG retrieval pipeline:
     * Seed Entities → BFS Diffusion → DPR + Entity Bonus → PPR → Top-K
     *
     * The graph service handles keyword extraction, embedding computation,
     * and the full retrieval pipeline internally.
     */
    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (CollectionUtils.isEmpty(request.getKnowledgeIds())) {
            return Collections.emptyList();
        }
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();
        log.debug("LinearRAG search for query: {}", request.getQuery());

        List<TextChunkVO> results = graphService.retrieve(
                request.getKnowledgeIds(),
                request.getQuery(),
                request.getExcludeParagraphIds(),
                request.getExcludeDocumentIds(),
                request.getTopK(),
                request.getMinScore()
        );

        log.debug("LinearRAG search completed in {}ms, found {} results",
                System.currentTimeMillis() - startTime, results.size());
        return results;
    }
}
