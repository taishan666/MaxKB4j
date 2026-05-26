package com.maxkb4j.knowledge.linearrag;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.linearrag.entity.GraphEntityNode;
import com.maxkb4j.knowledge.linearrag.entity.GraphSentenceNode;
import com.maxkb4j.knowledge.linearrag.model.GraphNode;
import com.maxkb4j.knowledge.linearrag.model.TriGraph;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.store.VectorStoreImpl;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
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
 * 1. Extract entities from paragraphs (using EntityExtractor: HanLP NER + TextRank + jieba)
 * 2. Store/update entity nodes in MongoDB
 * 3. Build the Tri-Graph with sentence + entity embeddings for retrieval
 * 4. Provide LinearRAG retrieval pipeline (seed entities → BFS → DPR via pgvector → PPR)
 *
 * Note: Paragraph embeddings are NOT stored in the graph — they live in pgvector
 * (managed by VectorStoreImpl) and are queried directly during DPR scoring.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinearRagGraphService {

    private final MongoTemplate mongoTemplate;
    private final ParagraphMapper paragraphMapper;
    private final KnowledgeModelService knowledgeModelService;
    private final VectorStoreImpl vectorStore;

    /** Config cache: knowledgeId -> LinearRagConfig */
    private final Cache<String, LinearRagConfig> configCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    /** Batch size for embedding computation */
    private static final int EMBEDDING_BATCH_SIZE = 64;

    // ==================== Graph Retrieval ====================

    /**
     * Execute LinearRAG retrieval for given knowledge bases.
     *
     * @param knowledgeIds        knowledge base IDs to search
     * @param query               the original query text
     * @param excludeParagraphIds paragraph IDs to exclude
     * @param excludeDocumentIds  document IDs to exclude
     * @param topK                maximum results
     * @param minScore            minimum score threshold
     * @return ranked list of TextChunkVO
     */
    public List<TextChunkVO> retrieve(List<String> knowledgeIds, String query,
                                       List<String> excludeParagraphIds, List<String> excludeDocumentIds,
                                       int topK, float minScore) {
        if (CollectionUtils.isEmpty(knowledgeIds) || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // Extract query keywords for seed entity matching
        List<String> queryKeywords = extractQueryKeywords(query);
        if (queryKeywords.isEmpty()) {
            log.debug("No keywords extracted from query: {}", query);
            return Collections.emptyList();
        }

        // Get embedding model and compute query embedding for BFS sentence similarity
        EmbeddingModel embeddingModel = getEmbeddingModel(knowledgeIds.getFirst());
        float[] queryEmbedding = computeQueryEmbedding(query, embeddingModel);
        // Build search context for pgvector DPR queries
        SearchRequest searchContext = new SearchRequest();
        searchContext.setKnowledgeIds(knowledgeIds);
        searchContext.setExcludeParagraphIds(excludeParagraphIds);
        searchContext.setExcludeDocumentIds(excludeDocumentIds);
        List<TextChunkVO> allResults = new ArrayList<>();
        for (String knowledgeId : knowledgeIds) {
            TriGraph graph = buildGraph(knowledgeId);
            if (graph.nodeCount() == 0) {
                log.debug("Empty graph for knowledge: {}", knowledgeId);
                continue;
            }
            LinearRagConfig config = getConfig(knowledgeId);
            LinearRagRetriever retriever = new LinearRagRetriever(graph, config, embeddingModel, vectorStore, searchContext);
            List<TextChunkVO> results = retriever.retrieve(query, queryKeywords, queryEmbedding, topK, minScore);
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
    }

    // ==================== Graph Building ====================

    /**
     * Get or create the LinearRagConfig for a knowledge base.
     */
    private LinearRagConfig getConfig(String knowledgeId) {
        return configCache.get(knowledgeId, k -> LinearRagConfig.builder().build());
    }

    /**
     * Load sentence nodes from MongoDB for the given paragraph IDs.
     */
    private List<GraphSentenceNode> loadSentenceNodes(String knowledgeId, Set<String> paragraphIds) {
        if (paragraphIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                        .and("paragraphId").in(paragraphIds)),
                GraphSentenceNode.class
        );
    }

    /**
     * Build the Tri-Graph from MongoDB data, including embedding loading/computation.
     */
    private TriGraph buildGraph(String knowledgeId) {
        long startTime = System.currentTimeMillis();
        log.debug("Building Tri-Graph for knowledge: {}", knowledgeId);

        // Load entities from MongoDB
        List<GraphEntityNode> entities = mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)),
                GraphEntityNode.class
        );

        if (CollectionUtils.isEmpty(entities)) {
            log.debug("No entities found for knowledge: {}", knowledgeId);
            return new TriGraph();
        }

        // Load paragraph content (ordered by ID for adjacency)
        Set<String> paragraphIds = entities.stream()
                .filter(e -> e.getParagraphIds() != null)
                .flatMap(e -> e.getParagraphIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, String> paragraphContentMap = new LinkedHashMap<>();
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

        // Load pre-extracted sentences from MongoDB
        List<GraphSentenceNode> sentenceNodes = loadSentenceNodes(knowledgeId, paragraphIds);

        // Build content map from pre-loaded sentence nodes (grouped by paragraphId, ordered by index)
        Map<String, List<String>> paragraphSentences = sentenceNodes.stream()
                .sorted(Comparator.comparingInt(GraphSentenceNode::getSentenceIndex))
                .collect(Collectors.groupingBy(
                        GraphSentenceNode::getParagraphId,
                        LinkedHashMap::new,
                        Collectors.mapping(GraphSentenceNode::getContent, Collectors.toList())
                ));

        // Build graph structure
        TriGraph graph = TriGraphBuilder.build(entities, paragraphContentMap, paragraphSentences);

        // Apply pre-loaded sentence embeddings (keyed by "s:paragraphId:index")
        for (GraphSentenceNode sn : sentenceNodes) {
            if (sn.getEmbedding() != null) {
                String sentenceId = "s:" + sn.getParagraphId() + ":" + sn.getSentenceIndex();
                graph.setSentenceEmbedding(sentenceId, sn.getEmbedding());
            }
        }

        // Apply pre-loaded entity embeddings (keyed by "e:name")
        for (GraphEntityNode en : entities) {
            if (en.getEmbedding() != null) {
                String entityId = "e:" + en.getName().toLowerCase();
                graph.setEntityEmbedding(entityId, en.getEmbedding());
            }
        }

        // Compute missing embeddings and persist to MongoDB (paragraph embeddings are in pgvector)
        try {
            computeAndStoreEmbeddings(graph, knowledgeId);
        } catch (Exception e) {
            log.warn("Failed to compute embeddings for knowledge {}: {}. Graph will work without embeddings.",
                    knowledgeId, e.getMessage());
        }

        log.debug("Built Tri-Graph for knowledge {} in {}ms: {}",
                knowledgeId, System.currentTimeMillis() - startTime, graph);
        return graph;
    }

    /**
     * Compute missing embeddings for sentence and entity nodes, and persist them to MongoDB.
     * Nodes that already have pre-loaded embeddings (from MongoDB) are skipped.
     */
    private void computeAndStoreEmbeddings(TriGraph graph, String knowledgeId) {
        EmbeddingModel embeddingModel = getEmbeddingModel(knowledgeId);
        if (embeddingModel == null) {
            log.warn("No embedding model available for knowledge: {}", knowledgeId);
            return;
        }

        long start = System.currentTimeMillis();

        // Compute only missing sentence embeddings
        List<GraphNode> sentencesNeedingEmbedding = graph.getNodesByType(GraphNode.NodeType.SENTENCE).stream()
                .filter(n -> graph.getSentenceEmbedding(n.getId()) == null)
                .collect(Collectors.toList());

        if (!sentencesNeedingEmbedding.isEmpty()) {
            batchEmbed(sentencesNeedingEmbedding, embeddingModel, graph::setSentenceEmbedding);
            persistSentenceEmbeddings(knowledgeId, sentencesNeedingEmbedding, graph);
        }

        // Compute only missing entity embeddings
        List<GraphNode> entitiesNeedingEmbedding = graph.getNodesByType(GraphNode.NodeType.ENTITY).stream()
                .filter(n -> graph.getEntityEmbedding(n.getId()) == null)
                .collect(Collectors.toList());

        if (!entitiesNeedingEmbedding.isEmpty()) {
            batchEmbed(entitiesNeedingEmbedding, embeddingModel, graph::setEntityEmbedding);
            persistEntityEmbeddings(knowledgeId, entitiesNeedingEmbedding, graph);
        }

        log.debug("Computed embeddings in {}ms: {} new sentence, {} new entity (total: {} sentence, {} entity)",
                System.currentTimeMillis() - start,
                sentencesNeedingEmbedding.size(),
                entitiesNeedingEmbedding.size(),
                graph.getAllSentenceEmbeddings().size(),
                graph.getAllEntityEmbeddings().size());
    }

    /**
     * Persist newly computed sentence embeddings to MongoDB.
     */
    private void persistSentenceEmbeddings(String knowledgeId, List<GraphNode> sentences, TriGraph graph) {
        for (GraphNode sentence : sentences) {
            float[] embedding = graph.getSentenceEmbedding(sentence.getId());
            if (embedding == null) continue;

            // Parse sentenceId "s:paragraphId:index" back to paragraphId and index
            String id = sentence.getId();
            if (!id.startsWith("s:")) continue;
            String remainder = id.substring(2);
            int lastColon = remainder.lastIndexOf(':');
            if (lastColon <= 0) continue;

            String paragraphId = remainder.substring(0, lastColon);
            int index = Integer.parseInt(remainder.substring(lastColon + 1));

            Query query = Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                    .and("paragraphId").is(paragraphId)
                    .and("sentenceIndex").is(index));
            Update update = new Update().set("embedding", embedding).set("updateTime", new Date());
            mongoTemplate.updateFirst(query, update, GraphSentenceNode.class);
        }
    }

    /**
     * Persist newly computed entity embeddings to MongoDB.
     */
    private void persistEntityEmbeddings(String knowledgeId, List<GraphNode> entities, TriGraph graph) {
        for (GraphNode entity : entities) {
            float[] embedding = graph.getEntityEmbedding(entity.getId());
            if (embedding == null) continue;

            // Parse entityId "e:name" back to name
            String id = entity.getId();
            if (!id.startsWith("e:")) continue;
            String name = id.substring(2);

            Query query = Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                    .and("name").is(name));
            Update update = new Update().set("embedding", embedding).set("updateTime", new Date());
            mongoTemplate.updateFirst(query, update, GraphEntityNode.class);
        }
    }

    /**
     * Batch-embed a list of graph nodes and store results via callback.
     */
    private void batchEmbed(List<GraphNode> nodes, EmbeddingModel model, EmbeddingCallback callback) {
        if (nodes.isEmpty()) return;

        List<GraphNode> validNodes = nodes.stream()
                .filter(n -> n.getContent() != null && !n.getContent().isBlank())
                .collect(Collectors.toList());

        if (validNodes.isEmpty()) return;

        for (int i = 0; i < validNodes.size(); i += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(i + EMBEDDING_BATCH_SIZE, validNodes.size());
            List<GraphNode> batch = validNodes.subList(i, end);

            try {
                List<TextSegment> segments = batch.stream()
                        .map(n -> TextSegment.from(n.getContent()))
                        .collect(Collectors.toList());

                Response<List<Embedding>> response = model.embedAll(segments);
                List<Embedding> embeddings = response.content();

                for (int j = 0; j < batch.size(); j++) {
                    callback.store(batch.get(j).getId(), embeddings.get(j).vector());
                }
            } catch (Exception e) {
                log.warn("Failed to embed batch [{}-{}]: {}", i, end, e.getMessage());
            }
        }
    }

    /**
     * Get embedding model for a knowledge base.
     */
    private EmbeddingModel getEmbeddingModel(String knowledgeId) {
        try {
            return knowledgeModelService.getEmbeddingModel(knowledgeId);
        } catch (Exception e) {
            log.warn("Failed to get embedding model for knowledge {}: {}", knowledgeId, e.getMessage());
            return null;
        }
    }

    /**
     * Compute query embedding for BFS sentence similarity.
     */
    private float[] computeQueryEmbedding(String query, EmbeddingModel embeddingModel) {
        if (embeddingModel == null || query == null || query.isBlank()) return null;
        try {
            Response<Embedding> response = embeddingModel.embed(query);
            return response.content().vector();
        } catch (Exception e) {
            log.warn("Failed to compute query embedding: {}", e.getMessage());
            return null;
        }
    }

    // ==================== Entity Extraction & Storage ====================

    /**
     * Extract entities from paragraphs and store in MongoDB.
     * Uses EntityExtractor (HanLP NER + TextRank + jieba) for lightweight entity extraction.
     */
    public void extractAndStoreEntities(String knowledgeId, String documentId, List<ParagraphEntity> paragraphs) {
        if (CollectionUtils.isEmpty(paragraphs)) return;

        long startTime = System.currentTimeMillis();
        log.info("Extracting LinearRAG entities and sentences for document: {}", documentId);

        List<GraphEntityNode> existingEntities = mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)),
                GraphEntityNode.class
        );
        Map<String, GraphEntityNode> entityMap = existingEntities.stream()
                .collect(Collectors.toMap(GraphEntityNode::getName, e -> e, (a, b) -> a));

        List<GraphEntityNode> updatedEntities = new ArrayList<>();
        int sentenceCount = 0;

        // Get embedding model for pre-computing embeddings
        EmbeddingModel embeddingModel = null;
        try {
            embeddingModel = knowledgeModelService.getEmbeddingModel(knowledgeId);
        } catch (Exception e) {
            log.warn("Failed to get embedding model for knowledge {}: {}. Embeddings will be computed on first retrieval.",
                    knowledgeId, e.getMessage());
        }

        for (ParagraphEntity paragraph : paragraphs) {
            if (paragraph.getContent() == null || paragraph.getContent().isBlank()) continue;

            String text = (paragraph.getTitle() != null ? paragraph.getTitle() + " " : "") + paragraph.getContent();
            Set<String> entities = extractEntities(text);

            for (String entityName : entities) {
                String normalizedName = entityName.toLowerCase().trim();
                GraphEntityNode existing = entityMap.get(normalizedName);

                if (existing != null) {
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
                    GraphEntityNode newNode = GraphEntityNode.builder()
                            .name(normalizedName)
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

            // Extract and store sentences for this paragraph (with embeddings)
            sentenceCount += extractAndStoreSentences(knowledgeId, documentId, paragraph.getId(), text, embeddingModel);
        }

        // Save entities to MongoDB (upsert)
        for (GraphEntityNode entity : updatedEntities) {
            try {
                if (entity.getId() != null) {
                    Query query = Query.query(Criteria.where("_id").is(entity.getId()));
                    Update update = new Update()
                            .set("paragraphIds", entity.getParagraphIds())
                            .set("documentIds", entity.getDocumentIds())
                            .set("frequency", entity.getFrequency())
                            .set("updateTime", new Date());
                    mongoTemplate.updateFirst(query, update, GraphEntityNode.class);
                } else {
                    mongoTemplate.insert(entity);
                }
            } catch (Exception e) {
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

        // Compute embeddings for entities that don't have one yet
        if (embeddingModel != null) {
            List<GraphEntityNode> entitiesNeedingEmbedding = updatedEntities.stream()
                    .filter(e -> e.getEmbedding() == null)
                    .collect(Collectors.toList());
            if (!entitiesNeedingEmbedding.isEmpty()) {
                List<GraphNode> entityGraphNodes = entitiesNeedingEmbedding.stream()
                        .map(e -> new GraphNode("e:" + e.getName(), e.getName(), GraphNode.NodeType.ENTITY, e.getName()))
                        .collect(Collectors.toList());
                batchEmbed(entityGraphNodes, embeddingModel, (nodeId, embedding) -> {
                    String name = nodeId.substring(2);
                    entitiesNeedingEmbedding.stream()
                            .filter(e -> e.getName().equals(name))
                            .findFirst()
                            .ifPresent(e -> e.setEmbedding(embedding));
                });
                // Persist entity embeddings to MongoDB
                for (GraphEntityNode entity : entitiesNeedingEmbedding) {
                    if (entity.getEmbedding() == null) continue;
                    try {
                        Query query = Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                                .and("name").is(entity.getName()));
                        Update update = new Update().set("embedding", entity.getEmbedding())
                                .set("updateTime", new Date());
                        mongoTemplate.updateFirst(query, update, GraphEntityNode.class);
                    } catch (Exception e) {
                        log.warn("Failed to persist embedding for entity {}: {}", entity.getName(), e.getMessage());
                    }
                }
            }
        }

        log.info("Extracted {} entities and {} sentences from document {} in {}ms",
                updatedEntities.size(), sentenceCount, documentId, System.currentTimeMillis() - startTime);
    }

    /**
     * Extract sentences from paragraph text and store in MongoDB.
     * Deletes existing sentences for the paragraph first, then inserts new ones.
     *
     * @return number of sentences stored
     */
    private int extractAndStoreSentences(String knowledgeId, String documentId, String paragraphId, String text,
                                          EmbeddingModel embeddingModel) {
        // Remove existing sentences for this paragraph (content may have changed)
        mongoTemplate.remove(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                        .and("paragraphId").is(paragraphId)),
                GraphSentenceNode.class
        );

        List<String> sentences = TriGraphBuilder.splitIntoSentences(text);
        if (sentences.isEmpty()) return 0;

        Date now = new Date();
        for (int i = 0; i < sentences.size(); i++) {
            GraphSentenceNode sentenceNode = GraphSentenceNode.builder()
                    .content(sentences.get(i))
                    .paragraphId(paragraphId)
                    .sentenceIndex(i)
                    .knowledgeId(knowledgeId)
                    .documentId(documentId)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            try {
                mongoTemplate.insert(sentenceNode);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("duplicate key")) {
                    log.debug("Sentence already exists for paragraph {} index {}", paragraphId, i);
                } else {
                    log.warn("Failed to save sentence for paragraph {} index {}: {}", paragraphId, i, e.getMessage());
                }
            }
        }

        // Compute and store sentence embeddings if embedding model is available
        if (embeddingModel != null) {
            List<GraphNode> sentenceGraphNodes = new ArrayList<>();
            for (int i = 0; i < sentences.size(); i++) {
                String s = sentences.get(i);
                sentenceGraphNodes.add(new GraphNode("s:" + paragraphId + ":" + i, s,
                        GraphNode.NodeType.SENTENCE, s));
            }
            batchEmbed(sentenceGraphNodes, embeddingModel, (nodeId, embedding) -> {
                // Parse nodeId "s:paragraphId:index" back to paragraphId and index
                String id = nodeId;
                if (!id.startsWith("s:")) return;
                String remainder = id.substring(2);
                int lastColon = remainder.lastIndexOf(':');
                if (lastColon <= 0) return;
                int index = Integer.parseInt(remainder.substring(lastColon + 1));
                Query q = Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                        .and("paragraphId").is(paragraphId)
                        .and("sentenceIndex").is(index));
                Update u = new Update().set("embedding", embedding).set("updateTime", new Date());
                try {
                    mongoTemplate.updateFirst(q, u, GraphSentenceNode.class);
                } catch (Exception e) {
                    log.warn("Failed to persist sentence embedding for paragraph {} index {}: {}",
                            paragraphId, index, e.getMessage());
                }
            });
        }

        return sentences.size();
    }

    /**
     * Extract entities from text using EntityExtractor.
     */
    public Set<String> extractEntities(String text) {
        return EntityExtractor.extractEntities(text);
    }

    /**
     * Extract keywords from a query string for retrieval.
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

        // Delete sentence nodes for these paragraphs
        mongoTemplate.remove(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                        .and("paragraphId").in(paragraphIds)),
                GraphSentenceNode.class
        );

        // Update or remove entity nodes
        List<GraphEntityNode> entities = mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                        .and("paragraphIds").in(paragraphIds)),
                GraphEntityNode.class
        );

        for (GraphEntityNode entity : entities) {
            entity.getParagraphIds().removeAll(paragraphIds);
            if (entity.getParagraphIds().isEmpty()) {
                mongoTemplate.remove(Query.query(Criteria.where("_id").is(entity.getId())), GraphEntityNode.class);
            } else {
                Query query = Query.query(Criteria.where("_id").is(entity.getId()));
                Update update = new Update().set("paragraphIds", entity.getParagraphIds());
                mongoTemplate.updateFirst(query, update, GraphEntityNode.class);
            }
        }
    }

    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (CollectionUtils.isEmpty(documentIds)) return;

        // Delete sentence nodes for these documents
        mongoTemplate.remove(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)
                        .and("documentId").in(documentIds)),
                GraphSentenceNode.class
        );

        // Update or remove entity nodes
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
    }

    public void deleteByKnowledgeId(String knowledgeId) {
        mongoTemplate.remove(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)),
                GraphSentenceNode.class
        );
        mongoTemplate.remove(
                Query.query(Criteria.where("knowledgeId").is(knowledgeId)),
                GraphEntityNode.class
        );
    }

    // ==================== Inner Interfaces ====================

    @FunctionalInterface
    private interface EmbeddingCallback {
        void store(String nodeId, float[] embedding);
    }
}
