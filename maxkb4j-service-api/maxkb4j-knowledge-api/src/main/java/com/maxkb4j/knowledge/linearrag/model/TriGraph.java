package com.maxkb4j.knowledge.linearrag.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tri-Graph data structure for LinearRAG.
 * Holds three types of nodes (Entity, Sentence, Paragraph) and their relationships.
 *
 * Edge types and weights per the LinearRAG paper:
 * - Entity↔Paragraph: weight = count(entity,passage) / total_entities_in_passage
 * - Paragraph↔Paragraph: weight = 1.0 (adjacent paragraphs by index prefix)
 * - Entity↔Sentence: tracked via bidirectional index (used in BFS, not in PPR graph)
 *
 * Also stores embeddings for sentence similarity (BFS) and entity matching (seed selection).
 */
public class TriGraph {

    // ---- Nodes ----
    private final Map<String, GraphNode> nodes = new ConcurrentHashMap<>();

    // ---- Weighted adjacency for PPR (only entity↔paragraph and paragraph↔paragraph edges) ----
    // nodeId -> (neighborId -> weight)
    private final Map<String, Map<String, Double>> weightedAdjacency = new ConcurrentHashMap<>();

    // ---- Entity↔Sentence bidirectional index (used in BFS diffusion, not in PPR graph) ----
    private final Map<String, Set<String>> entityToSentences = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sentenceToEntities = new ConcurrentHashMap<>();

    // ---- Entity occurrence count per paragraph: entityId -> (paragraphId -> count) ----
    private final Map<String, Map<String, Integer>> entityParagraphOccurrences = new ConcurrentHashMap<>();

    // ---- Embeddings (populated during graph building) ----
    private final Map<String, float[]> sentenceEmbeddings = new ConcurrentHashMap<>();
    private final Map<String, float[]> entityEmbeddings = new ConcurrentHashMap<>();
    private final Map<String, float[]> paragraphEmbeddings = new ConcurrentHashMap<>();

    // ==================== Node Operations ====================

    public void addNode(GraphNode node) {
        nodes.put(node.getId(), node);
        weightedAdjacency.computeIfAbsent(node.getId(), k -> new ConcurrentHashMap<>());
    }

    public GraphNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    // ==================== Edge Operations ====================

    /**
     * Add a weighted edge to the PPR graph (entity↔paragraph or paragraph↔paragraph).
     * Only these edge types participate in PPR computation.
     */
    public void addWeightedEdge(String sourceId, String targetId, double weight) {
        weightedAdjacency.computeIfAbsent(sourceId, k -> new ConcurrentHashMap<>()).put(targetId, weight);
        weightedAdjacency.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>()).put(sourceId, weight);
    }

    /**
     * Get weighted neighbors for PPR traversal.
     * Returns neighborId -> edgeWeight map.
     */
    public Map<String, Double> getWeightedNeighbors(String nodeId) {
        return weightedAdjacency.getOrDefault(nodeId, Collections.emptyMap());
    }

    /**
     * Get edge weight between two nodes. Returns 0 if no edge exists.
     */
    public double getEdgeWeight(String sourceId, String targetId) {
        Map<String, Double> neighbors = weightedAdjacency.get(sourceId);
        if (neighbors == null) return 0.0;
        return neighbors.getOrDefault(targetId, 0.0);
    }

    // ==================== Entity↔Sentence Bidirectional Index ====================

    /**
     * Link an entity to a sentence in the bidirectional index.
     * Used during BFS diffusion (entity→sentence→entity path).
     */
    public void linkEntityToSentence(String entityId, String sentenceId) {
        entityToSentences.computeIfAbsent(entityId, k -> ConcurrentHashMap.newKeySet()).add(sentenceId);
        sentenceToEntities.computeIfAbsent(sentenceId, k -> ConcurrentHashMap.newKeySet()).add(entityId);
    }

    /**
     * Get all sentences containing the given entity.
     */
    public Set<String> getEntitySentences(String entityId) {
        return entityToSentences.getOrDefault(entityId, Collections.emptySet());
    }

    /**
     * Get all entities contained in the given sentence.
     */
    public Set<String> getSentenceEntities(String sentenceId) {
        return sentenceToEntities.getOrDefault(sentenceId, Collections.emptySet());
    }

    // ==================== Entity Occurrence Tracking ====================

    /**
     * Record entity occurrence in a paragraph.
     */
    public void addEntityParagraphOccurrence(String entityId, String paragraphId, int count) {
        entityParagraphOccurrences
                .computeIfAbsent(entityId, k -> new ConcurrentHashMap<>())
                .merge(paragraphId, count, Integer::sum);
    }

    /**
     * Get the number of times an entity appears in a specific paragraph.
     */
    public int getEntityOccurrenceInParagraph(String entityId, String paragraphId) {
        Map<String, Integer> occurrences = entityParagraphOccurrences.get(entityId);
        if (occurrences == null) return 0;
        return occurrences.getOrDefault(paragraphId, 0);
    }

    /**
     * Get all paragraphs where an entity appears.
     */
    public Set<String> getEntityParagraphs(String entityId) {
        Map<String, Integer> occurrences = entityParagraphOccurrences.get(entityId);
        if (occurrences == null) return Collections.emptySet();
        return occurrences.keySet();
    }

    // ==================== Embedding Storage ====================

    public void setSentenceEmbedding(String sentenceId, float[] embedding) {
        sentenceEmbeddings.put(sentenceId, embedding);
    }

    public float[] getSentenceEmbedding(String sentenceId) {
        return sentenceEmbeddings.get(sentenceId);
    }

    public Map<String, float[]> getAllSentenceEmbeddings() {
        return Collections.unmodifiableMap(sentenceEmbeddings);
    }

    public void setEntityEmbedding(String entityId, float[] embedding) {
        entityEmbeddings.put(entityId, embedding);
    }

    public float[] getEntityEmbedding(String entityId) {
        return entityEmbeddings.get(entityId);
    }

    public Map<String, float[]> getAllEntityEmbeddings() {
        return Collections.unmodifiableMap(entityEmbeddings);
    }

    public void setParagraphEmbedding(String paragraphId, float[] embedding) {
        paragraphEmbeddings.put(paragraphId, embedding);
    }

    public float[] getParagraphEmbedding(String paragraphId) {
        return paragraphEmbeddings.get(paragraphId);
    }

    public Map<String, float[]> getAllParagraphEmbeddings() {
        return Collections.unmodifiableMap(paragraphEmbeddings);
    }

    // ==================== Query Operations ====================

    public Set<String> getNeighbors(String nodeId) {
        return weightedAdjacency.getOrDefault(nodeId, Collections.emptyMap()).keySet();
    }

    public List<GraphNode> getNodesByType(GraphNode.NodeType type) {
        return nodes.values().stream()
                .filter(n -> n.getType() == type)
                .collect(Collectors.toList());
    }

    public Set<String> getAllEntityIds() {
        return nodes.values().stream()
                .filter(n -> n.getType() == GraphNode.NodeType.ENTITY)
                .map(GraphNode::getId)
                .collect(Collectors.toSet());
    }

    public Set<String> getAllParagraphIds() {
        return nodes.values().stream()
                .filter(n -> n.getType() == GraphNode.NodeType.PARAGRAPH)
                .map(GraphNode::getId)
                .collect(Collectors.toSet());
    }

    public Set<String> getAllSentenceIds() {
        return nodes.values().stream()
                .filter(n -> n.getType() == GraphNode.NodeType.SENTENCE)
                .map(GraphNode::getId)
                .collect(Collectors.toSet());
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return weightedAdjacency.values().stream().mapToInt(Map::size).sum() / 2;
    }

    /**
     * Find entity nodes whose name matches any of the given keywords (case-insensitive).
     */
    public List<GraphNode> findEntitiesByKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> lowerKeywords = keywords.stream().map(String::toLowerCase).collect(Collectors.toSet());
        return nodes.values().stream()
                .filter(n -> n.getType() == GraphNode.NodeType.ENTITY)
                .filter(n -> {
                    String lowerName = n.getName().toLowerCase();
                    return lowerKeywords.stream().anyMatch(kw -> lowerName.contains(kw) || kw.contains(lowerName));
                })
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        int entities = getNodesByType(GraphNode.NodeType.ENTITY).size();
        int sentences = getNodesByType(GraphNode.NodeType.SENTENCE).size();
        int paragraphs = getNodesByType(GraphNode.NodeType.PARAGRAPH).size();
        return "TriGraph{entities=" + entities + ", sentences=" + sentences +
                ", paragraphs=" + paragraphs + ", edges=" + edgeCount() +
                ", sentenceEmbeddings=" + sentenceEmbeddings.size() +
                ", entityEmbeddings=" + entityEmbeddings.size() + "}";
    }
}
