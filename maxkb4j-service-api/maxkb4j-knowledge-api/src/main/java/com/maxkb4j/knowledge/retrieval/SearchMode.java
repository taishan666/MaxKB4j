package com.maxkb4j.knowledge.retrieval;

/**
 * Search mode enumeration for different retrieval strategies
 */
public enum SearchMode {

    /**
     * Vector similarity search using embeddings
     */
    VECTOR,

    /**
     * Full-text search using text indexing
     */
    FULL_TEXT,

    /**
     * Hybrid search combining vector and full-text search
     */
    HYBRID,
    /**
     * LinearRAG-based retrieval using Tri-Graph (Entity-Sentence-Paragraph)
     * with LoSemB entity activation + Personalized PageRank passage ranking.
     * Zero-token graph construction, linear complexity.
     */
    LINEAR_GRAPH
}