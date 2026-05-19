package com.maxkb4j.knowledge.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.core.dto.DualKeywordResult;
import com.maxkb4j.knowledge.entity.*;
import com.maxkb4j.knowledge.mapper.GraphEntityMapper;
import com.maxkb4j.knowledge.mapper.GraphRelationshipMapper;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.GraphKeywordService;
import com.maxkb4j.knowledge.service.GraphStoreService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component("graphStore")
@RequiredArgsConstructor
public class GraphStoreImpl implements IDataStore {

    private final GraphEntityMapper graphEntityMapper;
    private final GraphRelationshipMapper graphRelationshipMapper;
    private final GraphStoreService graphStoreService;
    private final GraphKeywordService graphKeywordService;
    private final KnowledgeModelService knowledgeModelService;

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        // Graph store upsert is handled through GraphExtractionService, not through this path
        // This method exists to satisfy the IDataStore interface but graph data is managed separately
        log.debug("GraphStore upsert called - graph data is managed through GraphExtractionService");
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        // Graph store doesn't store problem-based data
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        // Graph store doesn't store problem-based data
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        graphStoreService.deleteByParagraphIds(knowledgeId, paragraphIds);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        graphStoreService.deleteByDocumentIds(knowledgeId, documentIds);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        graphStoreService.deleteByKnowledgeId(knowledgeId);
    }

    @Override
    public void updateActiveStatus(String knowledgeId, String paragraphId, boolean isActive) {
        // Toggle is_active on entities and relationships that map to this paragraph
        List<String> entityIds = graphStoreService.getEntityIdsByParagraphId(knowledgeId, paragraphId);
        List<String> relationshipIds = graphStoreService.getRelationshipIdsByParagraphId(knowledgeId, paragraphId);

        if (!CollectionUtils.isEmpty(entityIds)) {
            LambdaUpdateWrapper<GraphEntityEntity> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.set(GraphEntityEntity::getIsActive, isActive)
                    .in(GraphEntityEntity::getId, entityIds)
                    .eq(GraphEntityEntity::getKnowledgeId, knowledgeId);
            graphEntityMapper.update(updateWrapper);
        }

        if (!CollectionUtils.isEmpty(relationshipIds)) {
            LambdaUpdateWrapper<GraphRelationshipEntity> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.set(GraphRelationshipEntity::getIsActive, isActive)
                    .in(GraphRelationshipEntity::getId, relationshipIds)
                    .eq(GraphRelationshipEntity::getKnowledgeId, knowledgeId);
            graphRelationshipMapper.update(updateWrapper);
        }
    }

    /**
     * Dual-level graph retrieval: low-level (entity) + high-level (relationship/topic)
     */
    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (CollectionUtils.isEmpty(request.getKnowledgeIds())) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(request.getQuery())) {
            return Collections.emptyList();
        }

        String knowledgeId = request.getKnowledgeIds().get(0);

        // Step 1: Extract dual-level keywords
        DualKeywordResult keywords;
        if (StringUtils.isNotBlank(request.getChatModelId())) {
            keywords = graphKeywordService.extractDualKeywords(request.getChatModelId(), request.getQuery());
        } else {
            keywords = graphKeywordService.fallbackKeywordExtraction(request.getQuery());
        }

        if (keywords == null || (CollectionUtils.isEmpty(keywords.getLowLevelKeywords())
                && CollectionUtils.isEmpty(keywords.getHighLevelKeywords()))) {
            return Collections.emptyList();
        }

        log.debug("Graph search keywords - low: {}, high: {}", keywords.getLowLevelKeywords(), keywords.getHighLevelKeywords());

        // Step 2: Run dual-level retrieval in parallel
        CompletableFuture<List<TextChunkVO>> lowLevelFuture = CompletableFuture.supplyAsync(
                () -> lowLevelRetrieval(knowledgeId, request, keywords.getLowLevelKeywords()));
        CompletableFuture<List<TextChunkVO>> highLevelFuture = CompletableFuture.supplyAsync(
                () -> highLevelRetrieval(knowledgeId, request, keywords.getHighLevelKeywords()));

        // Step 3: Merge results
        List<TextChunkVO> lowLevelResults = lowLevelFuture.join();
        List<TextChunkVO> highLevelResults = highLevelFuture.join();

        return mergeResults(lowLevelResults, highLevelResults, request.getTopK());
    }

    /**
     * Low-level retrieval: entity-specific search
     * 1. Match entities by name similarity to low-level keywords
     * 2. Find neighbor relationships connected to matched entities
     * 3. Collect paragraph IDs from entity and relationship mappings
     */
    private List<TextChunkVO> lowLevelRetrieval(String knowledgeId, SearchRequest request, List<String> lowLevelKeywords) {
        if (CollectionUtils.isEmpty(lowLevelKeywords)) {
            return Collections.emptyList();
        }

        // Match entities by name (exact + LIKE)
        List<GraphEntityEntity> matchedEntities = new ArrayList<>();
        for (String keyword : lowLevelKeywords) {
            List<GraphEntityEntity> entities = graphStoreService.findEntitiesByNameLike(knowledgeId, keyword);
            matchedEntities.addAll(entities);
        }
        // Also try exact name match
        List<GraphEntityEntity> exactMatches = graphStoreService.findEntitiesByNames(knowledgeId, lowLevelKeywords);
        matchedEntities.addAll(exactMatches);

        // Deduplicate entities
        Map<String, GraphEntityEntity> entityMap = new LinkedHashMap<>();
        for (GraphEntityEntity entity : matchedEntities) {
            entityMap.put(entity.getId(), entity);
        }

        if (entityMap.isEmpty()) {
            // Fallback: try vector similarity search on entity embeddings
            try {
                EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(knowledgeId);
                if (embeddingModel != null) {
                    Response<Embedding> queryEmbedding = embeddingModel.embed(request.getQuery());
                    float[] queryVector = queryEmbedding.content().vector();
                    List<String> excludeDocIds = request.getExcludeDocumentIds();
                    matchedEntities = graphEntityMapper.entityVectorSearch(
                            request.getKnowledgeIds(), excludeDocIds,
                            request.getTopK(), request.getMinScore(),
                            queryVector, embeddingModel.dimension());
                    for (GraphEntityEntity entity : matchedEntities) {
                        entityMap.put(entity.getId(), entity);
                    }
                }
            } catch (Exception e) {
                log.warn("Entity vector search fallback failed: {}", e.getMessage());
            }
        }

        if (entityMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> entityIds = new ArrayList<>(entityMap.keySet());

        // Find neighbor relationships connected to these entities
        List<GraphRelationshipEntity> neighborRelationships = graphStoreService.getNeighborRelationships(knowledgeId, entityIds);

        // Collect paragraph IDs from entity and relationship mappings
        List<String> entityParagraphIds = graphStoreService.getParagraphIdsByEntityIds(entityIds, knowledgeId);
        List<String> relationshipParagraphIds = Collections.emptyList();
        if (!CollectionUtils.isEmpty(neighborRelationships)) {
            List<String> relationshipIds = neighborRelationships.stream()
                    .map(GraphRelationshipEntity::getId).toList();
            relationshipParagraphIds = graphStoreService.getParagraphIdsByRelationshipIds(relationshipIds, knowledgeId);
        }

        // Build scored results
        // Entities matched by keyword get a base score, relationships extend the context
        float entityMatchScore = 0.7f;  // Score for direct entity match
        float relationshipScore = 0.5f;  // Score for neighbor relationship context

        Map<String, Float> paragraphScoreMap = new LinkedHashMap<>();
        for (String pid : entityParagraphIds) {
            paragraphScoreMap.merge(pid, entityMatchScore, Float::max);
        }
        for (String pid : relationshipParagraphIds) {
            paragraphScoreMap.merge(pid, relationshipScore, Float::max);
        }

        // Filter by exclude lists and min score
        List<TextChunkVO> results = new ArrayList<>();
        Set<String> excludeParagraphIds = request.getExcludeParagraphIds() != null ?
                new HashSet<>(request.getExcludeParagraphIds()) : Collections.emptySet();
        Set<String> excludeDocumentIds = request.getExcludeDocumentIds() != null ?
                new HashSet<>(request.getExcludeDocumentIds()) : Collections.emptySet();

        for (Map.Entry<String, Float> entry : paragraphScoreMap.entrySet()) {
            String paragraphId = entry.getKey();
            float score = entry.getValue();
            if (!excludeParagraphIds.contains(paragraphId) && score >= request.getMinScore()) {
                results.add(new TextChunkVO(paragraphId, score));
            }
        }

        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return results;
    }

    /**
     * High-level retrieval: topic/theme-level search
     * 1. Match relationships by keywords (topic matching)
     * 2. Collect paragraph IDs from relationship mappings
     * 3. Also collect entities connected to matched relationships
     */
    private List<TextChunkVO> highLevelRetrieval(String knowledgeId, SearchRequest request, List<String> highLevelKeywords) {
        if (CollectionUtils.isEmpty(highLevelKeywords)) {
            return Collections.emptyList();
        }

        // Match relationships by keywords
        List<GraphRelationshipEntity> matchedRelationships = graphStoreService.findRelationshipsByKeywords(knowledgeId, highLevelKeywords);

        if (CollectionUtils.isEmpty(matchedRelationships)) {
            // Fallback: try vector similarity search on relationship embeddings
            try {
                EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(knowledgeId);
                if (embeddingModel != null) {
                    Response<Embedding> queryEmbedding = embeddingModel.embed(request.getQuery());
                    float[] queryVector = queryEmbedding.content().vector();
                    List<String> excludeDocIds = request.getExcludeDocumentIds();
                    matchedRelationships = graphRelationshipMapper.relationshipVectorSearch(
                            request.getKnowledgeIds(), excludeDocIds,
                            request.getTopK(), request.getMinScore(),
                            queryVector, embeddingModel.dimension());
                }
            } catch (Exception e) {
                log.warn("Relationship vector search fallback failed: {}", e.getMessage());
            }
        }

        if (CollectionUtils.isEmpty(matchedRelationships)) {
            return Collections.emptyList();
        }

        // Collect source/target entities of matched relationships
        List<String> entityIds = matchedRelationships.stream()
                .flatMap(r -> Stream.of(r.getSourceEntityId(), r.getTargetEntityId()))
                .distinct()
                .toList();

        // Collect paragraph IDs
        List<String> relationshipIds = matchedRelationships.stream()
                .map(GraphRelationshipEntity::getId).toList();
        List<String> relationshipParagraphIds = graphStoreService.getParagraphIdsByRelationshipIds(relationshipIds, knowledgeId);

        List<String> entityParagraphIds = Collections.emptyList();
        if (!CollectionUtils.isEmpty(entityIds)) {
            entityParagraphIds = graphStoreService.getParagraphIdsByEntityIds(entityIds, knowledgeId);
        }

        // Build scored results
        float relationshipMatchScore = 0.8f;  // Score for direct relationship match
        float entityContextScore = 0.4f;  // Score for connected entity context

        Map<String, Float> paragraphScoreMap = new LinkedHashMap<>();
        for (String pid : relationshipParagraphIds) {
            paragraphScoreMap.merge(pid, relationshipMatchScore, Float::max);
        }
        for (String pid : entityParagraphIds) {
            paragraphScoreMap.merge(pid, entityContextScore, Float::max);
        }

        // Filter by exclude lists and min score
        List<TextChunkVO> results = new ArrayList<>();
        Set<String> excludeParagraphIds = request.getExcludeParagraphIds() != null ?
                new HashSet<>(request.getExcludeParagraphIds()) : Collections.emptySet();

        for (Map.Entry<String, Float> entry : paragraphScoreMap.entrySet()) {
            String paragraphId = entry.getKey();
            float score = entry.getValue();
            if (!excludeParagraphIds.contains(paragraphId) && score >= request.getMinScore()) {
                results.add(new TextChunkVO(paragraphId, score));
            }
        }

        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return results;
    }

    /**
     * Merge low-level and high-level results, deduplicate by paragraphId keeping max score
     */
    private List<TextChunkVO> mergeResults(List<TextChunkVO> lowLevelResults, List<TextChunkVO> highLevelResults, int topK) {
        Map<String, Float> mergedMap = new LinkedHashMap<>();

        for (TextChunkVO result : lowLevelResults) {
            mergedMap.merge(result.getParagraphId(), result.getScore(), Float::max);
        }
        for (TextChunkVO result : highLevelResults) {
            mergedMap.merge(result.getParagraphId(), result.getScore(), Float::max);
        }

        List<TextChunkVO> results = mergedMap.entrySet().stream()
                .map(e -> new TextChunkVO(e.getKey(), e.getValue()))
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        int end = Math.min(topK, results.size());
        return results.subList(0, end);
    }
}