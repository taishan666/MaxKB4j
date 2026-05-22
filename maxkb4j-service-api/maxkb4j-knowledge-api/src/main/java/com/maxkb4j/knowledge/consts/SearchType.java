package com.maxkb4j.knowledge.consts;


public interface SearchType {

    String EMBEDDING="embedding";
    String FULL_TEXT ="keywords";
    String HYBRID="hybrid";
    /**
     * LinearRAG-based graph retrieval.
     * Uses Tri-Graph (Entity-Sentence-Paragraph) with LoSemB + PPR two-stage retrieval.
     * Zero-token graph construction (no LLM calls needed for indexing).
     */
    String LINEAR_GRAPH="linear_graph";

}
