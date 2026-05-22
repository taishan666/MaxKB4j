package com.maxkb4j.knowledge.linearrag.model;

import lombok.Getter;

/**
 * Represents an edge in the LinearRAG Tri-Graph.
 * Edge types represent the structural relationships between nodes.
 */
@Getter
public class GraphEdge {

    public enum EdgeType {
        /** Entity appears in a sentence */
        ENTITY_IN_SENTENCE,
        /** Entity appears in a paragraph (via any sentence) */
        ENTITY_IN_PARAGRAPH,
        /** Sentence belongs to a paragraph */
        SENTENCE_IN_PARAGRAPH,
        /** Two entities co-occur in the same sentence */
        ENTITY_CO_OCCURRENCE
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
