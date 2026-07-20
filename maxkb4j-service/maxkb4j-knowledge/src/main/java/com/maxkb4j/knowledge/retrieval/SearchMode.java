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
    HYBRID
}