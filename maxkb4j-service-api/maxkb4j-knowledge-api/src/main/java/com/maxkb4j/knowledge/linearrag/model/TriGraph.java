package com.maxkb4j.knowledge.linearrag.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tri-Graph data structure for LinearRAG.
 * Holds three types of nodes (Entity, Sentence, Paragraph) and their relationships.
 * Supports efficient adjacency lookups for PPR traversal.
 */
public class TriGraph {

    private final Map<String, GraphNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> adjacencyList = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> entityToSentences = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> entityToParagraphs = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sentenceToParagraphs = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> entityCoOccurrence = new ConcurrentHashMap<>();

    public void addNode(GraphNode node) {
        nodes.put(node.getId(), node);
        adjacencyList.computeIfAbsent(node.getId(), k -> ConcurrentHashMap.newKeySet());
    }

    public void addEdge(GraphEdge edge) {
        adjacencyList.computeIfAbsent(edge.getSourceId(), k -> ConcurrentHashMap.newKeySet()).add(edge.getTargetId());
        adjacencyList.computeIfAbsent(edge.getTargetId(), k -> ConcurrentHashMap.newKeySet()).add(edge.getSourceId());

        switch (edge.getType()) {
            case ENTITY_IN_SENTENCE:
                entityToSentences.computeIfAbsent(edge.getSourceId(), k -> ConcurrentHashMap.newKeySet()).add(edge.getTargetId());
                break;
            case ENTITY_IN_PARAGRAPH:
                entityToParagraphs.computeIfAbsent(edge.getSourceId(), k -> ConcurrentHashMap.newKeySet()).add(edge.getTargetId());
                break;
            case SENTENCE_IN_PARAGRAPH:
                sentenceToParagraphs.computeIfAbsent(edge.getSourceId(), k -> ConcurrentHashMap.newKeySet()).add(edge.getTargetId());
                break;
            case ENTITY_CO_OCCURRENCE:
                entityCoOccurrence.computeIfAbsent(edge.getSourceId(), k -> ConcurrentHashMap.newKeySet()).add(edge.getTargetId());
                entityCoOccurrence.computeIfAbsent(edge.getTargetId(), k -> ConcurrentHashMap.newKeySet()).add(edge.getSourceId());
                break;
        }
    }

    public GraphNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Set<String> getNeighbors(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptySet());
    }

    public Set<String> getEntitySentences(String entityId) {
        return entityToSentences.getOrDefault(entityId, Collections.emptySet());
    }

    public Set<String> getEntityParagraphs(String entityId) {
        return entityToParagraphs.getOrDefault(entityId, Collections.emptySet());
    }

    public Set<String> getSentenceParagraphs(String sentenceId) {
        return sentenceToParagraphs.getOrDefault(sentenceId, Collections.emptySet());
    }

    public Set<String> getCoOccurringEntities(String entityId) {
        return entityCoOccurrence.getOrDefault(entityId, Collections.emptySet());
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
        return adjacencyList.values().stream().mapToInt(Set::size).sum() / 2;
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
                ", paragraphs=" + paragraphs + ", edges=" + edgeCount() + "}";
    }
}
