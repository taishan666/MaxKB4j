package com.maxkb4j.knowledge.linearrag.model;

import lombok.Getter;

/**
 * Represents an edge in the LinearRAG Tri-Graph.
 *
 * Edge types per the LinearRAG paper:
 * - ENTITY_TO_PARAGRAPH:    entity appears in paragraph, weight = count/total (used in PPR)
 * - PARAGRAPH_ADJACENCY:    adjacent paragraphs in document order, weight = 1.0 (used in PPR)
 * - ENTITY_IN_SENTENCE:     entity name appears in sentence text (used in BFS, not in PPR graph)
 */
@Getter
public class GraphEdge {

    public enum EdgeType {
        /** Entity appears in paragraph with frequency-based weight (PPR graph edge) */
        ENTITY_TO_PARAGRAPH,
        /** Adjacent paragraphs connected in document order (PPR graph edge, weight=1.0) */
        PARAGRAPH_ADJACENCY,
        /** Entity appears in sentence (BFS index, not a PPR graph edge) */
        ENTITY_IN_SENTENCE
    }

    private final String sourceId;
    private final String targetId;
    private final EdgeType type;
    private final double weight;

    public GraphEdge(String sourceId, String targetId, EdgeType type, double weight) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.weight = weight;
    }

    public GraphEdge(String sourceId, String targetId, EdgeType type) {
        this(sourceId, targetId, type, 1.0);
    }

    @Override
    public String toString() {
        return "GraphEdge{" + sourceId + " -> " + targetId + ", type=" + type + ", weight=" + weight + "}";
    }
}
