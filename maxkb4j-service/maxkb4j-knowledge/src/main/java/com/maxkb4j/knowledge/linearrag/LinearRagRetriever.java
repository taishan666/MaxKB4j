package com.maxkb4j.knowledge.linearrag;

import com.maxkb4j.knowledge.linearrag.model.GraphNode;
import com.maxkb4j.knowledge.linearrag.model.TriGraph;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LinearRAG two-stage retrieval implementation.
 * Stage 1 - Local Semantic Bridging (LoSemB):
 *   Activate relevant entity nodes from the query through semantic matching
 *   and graph-based expansion on the entity-sentence subgraph.
 * Stage 2 - PPR-based Passage Retrieval:
 *   Use activated entities as seeds for Personalized PageRank to compute
 *   global relevance scores for paragraph nodes.
 */
@Slf4j
public class LinearRagRetriever {

    private final TriGraph graph;
    private final PersonalizedPageRank ppr;

    /** Weight for exact entity name match */
    private static final double EXACT_MATCH_WEIGHT = 1.0;
    /** Weight for partial (substring) entity match */
    private static final double PARTIAL_MATCH_WEIGHT = 0.6;
    /** Weight for expanded entities (via co-occurrence) */
    private static final double EXPANSION_WEIGHT = 0.4;
    /** Maximum number of expansion hops in LoSemB */
    private static final int MAX_EXPANSION_HOPS = 2;
    /** Maximum number of entities to activate in LoSemB */
    private static final int MAX_ACTIVATED_ENTITIES = 30;

    public LinearRagRetriever(TriGraph graph) {
        this.graph = graph;
        this.ppr = new PersonalizedPageRank(graph);
    }

    /**
     * Execute two-stage LinearRAG retrieval.
     *
     * @param queryKeywords keywords extracted from the query
     * @param topK          maximum number of results
     * @param minScore      minimum score threshold
     * @return ranked list of TextChunkVO (paragraph ID + score)
     */
    public List<TextChunkVO> retrieve(List<String> queryKeywords, int topK, float minScore) {
        if (queryKeywords == null || queryKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("LinearRAG retrieval started with {} keywords, topK={}", queryKeywords.size(), topK);

        // Stage 1: Local Semantic Bridging - activate entities
        Map<String, Double> activatedEntities = localSemanticBridging(queryKeywords);

        if (activatedEntities.isEmpty()) {
            log.debug("No entities activated in LoSemB stage, falling back to keyword-based matching");
            return fallbackKeywordSearch(queryKeywords, topK, minScore);
        }

        log.debug("LoSemB activated {} entities", activatedEntities.size());

        // Stage 2: PPR-based passage retrieval
        Map<String, Double> paragraphScores = ppr.computeParagraphScores(activatedEntities);

        // Convert to TextChunkVO and filter
        return paragraphScores.entrySet().stream()
                .filter(e -> e.getValue() >= minScore)
                .limit(topK)
                .map(e -> {
                    // Strip "p:" prefix from paragraph ID
                    String paragraphId = e.getKey().startsWith("p:") ? e.getKey().substring(2) : e.getKey();
                    return new TextChunkVO(paragraphId, e.getValue().floatValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * Stage 1: Local Semantic Bridging (LoSemB)
     * Activates entity nodes relevant to the query through:
     * 1. Direct keyword matching (exact + partial)
     * 2. Graph-based expansion via entity co-occurrence
     *
     * @param queryKeywords keywords from the query
     * @return map of activated entity ID -> activation score
     */
    private Map<String, Double> localSemanticBridging(List<String> queryKeywords) {
        Map<String, Double> activatedEntities = new LinkedHashMap<>();

        // Step 1: Direct keyword matching
        Set<String> lowerKeywords = queryKeywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String entityId : graph.getAllEntityIds()) {
            GraphNode entityNode = graph.getNode(entityId);
            if (entityNode == null) continue;

            String lowerName = entityNode.getName().toLowerCase();

            // Exact match: keyword equals entity name
            if (lowerKeywords.contains(lowerName)) {
                activatedEntities.put(entityId, EXACT_MATCH_WEIGHT);
                continue;
            }

            // Partial match: keyword is substring of entity name or vice versa
            for (String keyword : lowerKeywords) {
                if (lowerName.contains(keyword) || keyword.contains(lowerName)) {
                    activatedEntities.put(entityId, Math.max(
                            activatedEntities.getOrDefault(entityId, 0.0),
                            PARTIAL_MATCH_WEIGHT
                    ));
                    break;
                }
            }
        }

        // Step 2: Graph-based expansion via co-occurrence
        Map<String, Double> expandedEntities = new LinkedHashMap<>(activatedEntities);
        Set<String> visited = new HashSet<>(activatedEntities.keySet());

        for (int hop = 0; hop < MAX_EXPANSION_HOPS; hop++) {
            Map<String, Double> newEntities = new HashMap<>();
            double decayFactor = EXPANSION_WEIGHT * Math.pow(0.5, hop);

            for (Map.Entry<String, Double> entry : activatedEntities.entrySet()) {
                String entityId = entry.getKey();
                double score = entry.getValue();

                Set<String> coEntities = graph.getCoOccurringEntities(entityId);
                for (String coEntityId : coEntities) {
                    if (!visited.contains(coEntityId)) {
                        double expandedScore = score * decayFactor;
                        newEntities.merge(coEntityId, expandedScore, Double::max);
                    }
                }
            }

            if (newEntities.isEmpty()) break;

            expandedEntities.putAll(newEntities);
            visited.addAll(newEntities.keySet());

            // For next hop, use newly discovered entities
            activatedEntities = newEntities;
        }

        // Limit to top N activated entities
        return expandedEntities.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_ACTIVATED_ENTITIES)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Fallback: when LoSemB finds no entities, do a simple keyword search
     * over paragraph content in the graph.
     */
    private List<TextChunkVO> fallbackKeywordSearch(List<String> queryKeywords, int topK, float minScore) {
        Map<String, Double> paragraphScores = new HashMap<>();
        Set<String> lowerKeywords = queryKeywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String paragraphId : graph.getAllParagraphIds()) {
            GraphNode paraNode = graph.getNode(paragraphId);
            if (paraNode == null || paraNode.getContent() == null) continue;

            String lowerContent = paraNode.getContent().toLowerCase();
            long matchCount = lowerKeywords.stream().filter(lowerContent::contains).count();

            if (matchCount > 0) {
                double score = (double) matchCount / lowerKeywords.size();
                paragraphScores.put(paragraphId, score);
            }
        }

        // Normalize
        double maxScore = paragraphScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxScore > 0) {
            paragraphScores.replaceAll((k, v) -> v / maxScore);
        }

        return paragraphScores.entrySet().stream()
                .filter(e -> e.getValue() >= minScore)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    String paraId = e.getKey().startsWith("p:") ? e.getKey().substring(2) : e.getKey();
                    return new TextChunkVO(paraId, e.getValue().floatValue());
                })
                .collect(Collectors.toList());
    }
}
