package com.maxkb4j.knowledge.linearrag;

import lombok.Builder;
import lombok.Data;

/**
 * LinearRAG hyperparameters as specified in the paper.
 * Controls BFS diffusion, PPR convergence, and passage scoring.
 */
@Data
@Builder
public class LinearRagConfig {

    /** BFS maximum diffusion rounds (default: 3) */
    @Builder.Default
    private int maxIterations = 3;

    /** BFS score pruning threshold — entities below this are not expanded (default: 0.5) */
    @Builder.Default
    private double iterationThreshold = 0.5;

    /** Number of most relevant sentences per entity per BFS round (default: 1) */
    @Builder.Default
    private int topKSentence = 1;

    /** DPR score weight coefficient in passage scoring formula (default: 1.5) */
    @Builder.Default
    private double passageRatio = 1.5;

    /** Paragraph node scaling factor in PPR reset distribution (default: 0.05) */
    @Builder.Default
    private double passageNodeWeight = 0.05;

    /** PPR damping factor — smaller means more reliance on reset distribution (default: 0.5) */
    @Builder.Default
    private double damping = 0.5;

    /** PPR maximum iterations (default: 100) */
    @Builder.Default
    private int pprMaxIter = 100;

    /** PPR convergence tolerance (default: 1e-6) */
    @Builder.Default
    private double pprTolerance = 1e-6;

    /** Number of top passages to return (default: 5) */
    @Builder.Default
    private int retrievalTopK = 5;

    /** Whether to enable attribute keyword boost for property queries (default: false) */
    @Builder.Default
    private boolean enableHybridAttributeFallback = false;

    /** Attribute keyword boost coefficient (default: 0.25) */
    @Builder.Default
    private double attributeKeywordBoost = 0.25;

    /** Maximum number of seed entities to use (default: 30) */
    @Builder.Default
    private int maxSeedEntities = 30;

    /**
     * Attribute keywords for property-based queries (English).
     * When a question contains these words, attribute keyword boost is applied.
     */
    public static final String[] ATTRIBUTE_KEYWORDS_EN = {
            "born", "where", "when", "founded", "located", "died",
            "birth", "death", "country", "city", "capital", "population"
    };

    /**
     * Attribute keywords for property-based queries (Chinese).
     */
    public static final String[] ATTRIBUTE_KEYWORDS_ZH = {
            "出生", "哪里", "什么时候", "创立", "位于", "去世",
            "生日", "国家", "城市", "首都", "人口", "面积", "建立"
    };
}
