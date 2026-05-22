package com.maxkb4j.knowledge.linearrag;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.linearrag.entity.GraphEntityNode;
import com.maxkb4j.knowledge.linearrag.model.TriGraph;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing the LinearRAG Tri-Graph.
 *
 * Responsibilities:
 * 1. Extract entities from paragraphs (using HanLP NER + TextRank + jieba triple strategy)
 * 2. Store/update entity nodes in MongoDB
 * 3. Build and cache the Tri-Graph for efficient retrieval
 * 4. Provide two-stage retrieval (LoSemB + PPR)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinearRagGraphService {

    private final MongoTemplate mongoTemplate;
    private final ParagraphMapper paragraphMapper;

    /** Graph cache: knowledgeId -> TriGraph (expires after 30 min of no access) */
    private final Cache<String, TriGraph> graphCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    // ==================== Graph Retrieval ====================

    /**
     * Execute LinearRAG two-stage retrieval for given knowledge bases.
     *
     * @param knowledgeIds       knowledge base IDs to search
     * @param queryKeywords      keywords extracted from query
     * @param excludeParagraphIds paragraph IDs to exclude
     * @param excludeDocumentIds  document IDs to exclude
     * @param topK               maximum results
     * @param minScore           minimum score threshold
     * @return ranked list of TextChunkVO
     */
    public List<TextChunkVO> retrieve(List<String> knowledgeIds, List<String> queryKeywords,
                                       List<String> excludeParagraphIds, List<String> excludeDocumentIds,
                                       int topK, float minScore) {
        if (CollectionUtils.isEmpty(knowledgeIds) || CollectionUtils.isEmpty(queryKeywords)) {
            return Collections.emptyList();
        }

        List<TextChunkVO> allResults = new ArrayList<>();

        for (String knowledgeId : knowledgeIds) {
            TriGraph graph = getOrBuildGraph(knowledgeId);
            if (graph.nodeCount() == 0) {
                log.debug("Empty graph for knowledge: {}", knowledgeId);
                continue;
            }

            LinearRagRetriever retriever = new LinearRagRetriever(graph);
            List<TextChunkVO> results = retriever.retrieve(queryKeywords, topK, minScore);
            allResults.addAll(results);
        }

        // Deduplicate and sort
        Map<String, Float> bestScores = new LinkedHashMap<>();
        for (TextChunkVO result : allResults) {
            bestScores.merge(result.getParagraphId(), result.getScore(), Math::max);
        }

        List<TextChunkVO> deduped = bestScores.entrySet().stream()
                .filter(e -> !isExcluded(e.getKey(), excludeParagraphIds, excludeDocumentIds))
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new TextChunkVO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        log.debug("LinearRAG retrieval returned {} results", deduped.size());
        return deduped;
    }

    private boolean isExcluded(String paragraphId, List<String> excludeParagraphIds, List<String> excludeDocumentIds) {
        return !CollectionUtils.isEmpty(excludeParagraphIds) && excludeParagraphIds.contains(paragraphId);
        // Document exclusion would require paragraph->document lookup, skip for performance
    }

    // ==================== Graph Building & Caching ====================

    /**
     * Get or build the Tri-Graph for a knowledge base.
     */
    public TriGraph getOrBuildGraph(String knowledgeId) {
        return graphCache.get(knowledgeId, this::buildGraph);
    }

    /**
     * Build the Tri-Graph from MongoDB data.
     */
    private TriGraph buildGraph(String knowledgeId) {
        long startTime = System.currentTimeMillis();
        log.debug("Building Tri-Graph for knowledge: {}", knowledgeId);

        // Load entities
        List<GraphEntityNode> entities = mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)),
                GraphEntityNode.class
        );

        if (CollectionUtils.isEmpty(entities)) {
            log.debug("No entities found for knowledge: {}", knowledgeId);
            return new TriGraph();
        }

        // Load paragraph content
        Set<String> paragraphIds = entities.stream()
                .filter(e -> e.getParagraphIds() != null)
                .flatMap(e -> e.getParagraphIds().stream())
                .collect(Collectors.toSet());

        Map<String, String> paragraphContentMap = new HashMap<>();
        if (!paragraphIds.isEmpty()) {
            List<ParagraphEntity> paragraphs = paragraphMapper.selectByIds(new ArrayList<>(paragraphIds));
            for (ParagraphEntity p : paragraphs) {
                if (p.getIsActive() != null && p.getIsActive()) {
                    String content = (p.getTitle() != null ? p.getTitle() + ": " : "") +
                            (p.getContent() != null ? p.getContent() : "");
                    paragraphContentMap.put(p.getId(), content);
                }
            }
        }

        TriGraph graph = TriGraphBuilder.build(entities, paragraphContentMap);
        log.debug("Built Tri-Graph for knowledge {} in {}ms: {}",
                knowledgeId, System.currentTimeMillis() - startTime, graph);
        return graph;
    }

    /**
     * Invalidate graph cache for a knowledge base.
     */
    public void invalidateGraph(String knowledgeId) {
        graphCache.invalidate(knowledgeId);
    }

    // ==================== Entity Extraction & Storage ====================

    /**
     * Extract entities from paragraphs and store in MongoDB.
     * Uses EntityExtractor (HanLP NER + TextRank + jieba) for lightweight entity extraction.
     *
     * @param knowledgeId  knowledge base ID
     * @param documentId   document ID
     * @param paragraphs   list of paragraphs to extract from
     */
    public void extractAndStoreEntities(String knowledgeId, String documentId, List<ParagraphEntity> paragraphs) {
        if (CollectionUtils.isEmpty(paragraphs)) return;

        long startTime = System.currentTimeMillis();
        log.info("Extracting LinearRAG entities for document: {}", documentId);

        // Load existing entities for this knowledge base
        List<GraphEntityNode> existingEntities = mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)),
                GraphEntityNode.class
        );
        Map<String, GraphEntityNode> entityMap = existingEntities.stream()
                .collect(Collectors.toMap(GraphEntityNode::getName, e -> e, (a, b) -> a));

        List<GraphEntityNode> updatedEntities = new ArrayList<>();

        for (ParagraphEntity paragraph : paragraphs) {
            if (paragraph.getContent() == null || paragraph.getContent().isBlank()) continue;

            String text = (paragraph.getTitle() != null ? paragraph.getTitle() + " " : "") + paragraph.getContent();
            Set<String> entities = extractEntities(text);

            for (String entityName : entities) {
                String normalizedName = entityName.toLowerCase().trim();
                GraphEntityNode existing = entityMap.get(normalizedName);

                if (existing != null) {
                    // Update existing entity
                    if (!existing.getParagraphIds().contains(paragraph.getId())) {
                        existing.getParagraphIds().add(paragraph.getId());
                    }
                    if (!existing.getDocumentIds().contains(documentId)) {
                        existing.getDocumentIds().add(documentId);
                    }
                    existing.setFrequency(existing.getFrequency() + 1);
                    existing.setUpdateTime(new Date());
                    updatedEntities.add(existing);
                } else {
                    // Create new entity
                    GraphEntityNode newNode = GraphEntityNode.builder()
                            .name(normalizedName)
                            .originalName(entityName)
                            .knowledgeId(knowledgeId)
                            .documentIds(new ArrayList<>(List.of(documentId)))
                            .paragraphIds(new ArrayList<>(List.of(paragraph.getId())))
                            .frequency(1)
                            .createTime(new Date())
                            .updateTime(new Date())
                            .build();
                    entityMap.put(normalizedName, newNode);
                    updatedEntities.add(newNode);
                }
            }
        }

        // Save to MongoDB (upsert)
        for (GraphEntityNode entity : updatedEntities) {
            try {
                if (entity.getId() != null) {
                    // Update existing
                    Query query = Query.query(Criteria.where("_id").is(entity.getId()));
                    Update update = new Update()
                            .set("paragraphIds", entity.getParagraphIds())
                            .set("documentIds", entity.getDocumentIds())
                            .set("frequency", entity.getFrequency())
                            .set("updateTime", new Date());
                    mongoTemplate.updateFirst(query, update, GraphEntityNode.class);
                } else {
                    // Insert new
                    mongoTemplate.insert(entity);
                }
            } catch (Exception e) {
                // Handle duplicate key (concurrent insert)
                if (e.getMessage() != null && e.getMessage().contains("duplicate key")) {
                    log.debug("Entity already exists, updating: {}", entity.getName());
                    Query query = Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                            .and("name").is(entity.getName()));
                    Update update = new Update()
                            .addToSet("paragraphIds").each(entity.getParagraphIds().toArray())
                            .addToSet("documentIds").each(entity.getDocumentIds().toArray())
                            .inc("frequency", entity.getFrequency())
                            .set("updateTime", new Date());
                    mongoTemplate.updateFirst(query, update, GraphEntityNode.class);
                } else {
                    log.warn("Failed to save entity {}: {}", entity.getName(), e.getMessage());
                }
            }
        }

        // Invalidate graph cache since entities changed
        invalidateGraph(knowledgeId);
        log.info("Extracted {} entities from document {} in {}ms",
                updatedEntities.size(), documentId, System.currentTimeMillis() - startTime);
    }

    /**
     * Extract entities from text using HanLP NER (lightweight named entity recognition).
     * Uses HanLP's CRF-based perceptron segmenter with POS tagging to identify
     * named entities (person names, locations, organizations, etc.) instead of
     * simple word segmentation. This provides more accurate entity extraction
     * aligned with the LinearRAG paper's approach.
     *
     * @param text input text
     * @return set of extracted entity names
     */
    public Set<String> extractEntities(String text) {
        return EntityExtractor.extractEntities(text);
    }

    /**
     * Extract keywords from a query string for retrieval.
     * Uses the same triple-strategy EntityExtractor for consistency with
     * document indexing. This ensures query terms match the entity names
     * stored in the Tri-Graph during LoSemB activation.
     */
    public List<String> extractQueryKeywords(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(EntityExtractor.extractEntities(query));
    }

    // ==================== CRUD Operations ====================

    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (CollectionUtils.isEmpty(paragraphIds)) return;

        // Remove paragraph IDs from entity nodes
        List<GraphEntityNode> entities = mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                        .and("paragraphIds").in(paragraphIds)),
                GraphEntityNode.class
        );

        for (GraphEntityNode entity : entities) {
            entity.getParagraphIds().removeAll(paragraphIds);
            if (entity.getParagraphIds().isEmpty()) {
                // Entity no longer appears in any paragraph, remove it
                mongoTemplate.remove(Query.query(Criteria.where("_id").is(entity.getId())), GraphEntityNode.class);
            } else {
                Query query = Query.query(Criteria.where("_id").is(entity.getId()));
                Update update = new Update().set("paragraphIds", entity.getParagraphIds());
                mongoTemplate.updateFirst(query, update, GraphEntityNode.class);
            }
        }

        invalidateGraph(knowledgeId);
    }

    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (CollectionUtils.isEmpty(documentIds)) return;

        List<GraphEntityNode> entities = mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                        .and("documentIds").in(documentIds)),
                GraphEntityNode.class
        );

        for (GraphEntityNode entity : entities) {
            entity.getDocumentIds().removeAll(documentIds);
            if (entity.getDocumentIds().isEmpty()) {
                mongoTemplate.remove(Query.query(Criteria.where("_id").is(entity.getId())), GraphEntityNode.class);
            } else {
                Query query = Query.query(Criteria.where("_id").is(entity.getId()));
                Update update = new Update().set("documentIds", entity.getDocumentIds());
                mongoTemplate.updateFirst(query, update, GraphEntityNode.class);
            }
        }

        invalidateGraph(knowledgeId);
    }

    public void deleteByKnowledgeId(String knowledgeId) {
        mongoTemplate.remove(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)),
                GraphEntityNode.class
        );
        invalidateGraph(knowledgeId);
    }
}
