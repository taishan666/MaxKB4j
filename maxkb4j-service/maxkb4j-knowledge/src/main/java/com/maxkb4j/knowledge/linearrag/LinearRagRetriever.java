package com.maxkb4j.knowledge.linearrag;

import com.maxkb4j.knowledge.linearrag.model.GraphNode;
import com.maxkb4j.knowledge.linearrag.model.TriGraph;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.store.VectorStoreImpl;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LinearRAG retrieval implementation per the paper specification.
 *
 * Retrieval pipeline:
 *   question
 *      │
 *      ▼
 *   getSeedEntities()       ← NER keywords → EmbeddingModel → cosine similarity vs entity embeddings
 *      │
 *      ├── seed entities found → graphSearchWithSeedEntities() → PPR → Top-K passages
 *      │
 *      └── no seed entities   → densePassageRetrieval()        → pgvector search → Top-K passages
 *
 * graphSearchWithSeedEntities():
 *   1. BFS entity score diffusion (entity → sentence → entity, multiplication decay)
 *      Uses sentence embeddings vs query embedding for sentence relevance scoring.
 *   2. DPR dense passage retrieval via pgvector + entity bonus + attribute keyword boost
 *   3. Combine entity + passage scores as PPR reset distribution
 *   4. Run Personalized PageRank → Top-K passages
 */
@Slf4j
public class LinearRagRetriever {

    private final TriGraph graph;
    private final LinearRagConfig config;
    private final PersonalizedPageRank ppr;
    private final EmbeddingModel embeddingModel;
    private final VectorStoreImpl vectorStore;
    private final SearchRequest searchContext;

    /** Number of DPR candidates to fetch from pgvector for scoring */
    private static final int DPR_CANDIDATE_COUNT = 50;

    public LinearRagRetriever(TriGraph graph, LinearRagConfig config,
                               EmbeddingModel embeddingModel,
                               VectorStoreImpl vectorStore, SearchRequest searchContext) {
        this.graph = graph;
        this.config = config;
        this.ppr = new PersonalizedPageRank(graph, config);
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.searchContext = searchContext;
    }

    /**
     * Execute the full LinearRAG retrieval pipeline.
     *
     * @param query          the question text
     * @param queryKeywords  keywords extracted from query (for NER/seed matching)
     * @param queryEmbedding embedding vector of the full query (for BFS sentence similarity)
     * @param topK           maximum number of results
     * @param minScore       minimum score threshold
     * @return ranked list of TextChunkVO (paragraph ID + score)
     */
    public List<TextChunkVO> retrieve(String query, List<String> queryKeywords, float[] queryEmbedding,
                                      int topK, float minScore) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        log.debug("LinearRAG retrieval started, query length={}, keywords={}, topK={}",
                query.length(), queryKeywords != null ? queryKeywords.size() : 0, topK);

        // Step 1: Find seed entities via keyword embedding × entity embedding similarity
        List<SeedEntity> seedEntities = getSeedEntities(queryKeywords);

        if (seedEntities.isEmpty()) {
            log.debug("No seed entities found, falling back to dense passage retrieval via pgvector");
            return densePassageRetrieval(query, topK, minScore);
        }

        log.debug("Found {} seed entities for graph search", seedEntities.size());

        // Step 2: Graph search with seed entities (BFS + DPR + PPR)
        return graphSearchWithSeedEntities(query, seedEntities, queryEmbedding, topK, minScore);
    }

    // ==================== Step 1: Seed Entity Matching ====================

    /**
     * Find seed entities by computing keyword embeddings and comparing against entity embeddings.
     *
     * Per the paper:
     *   question → NER → question entities
     *   question entities → EmbeddingModel.encode() → keyword embeddings
     *   entityEmbeddings × keywordEmbeddingsᵀ → similarity matrix
     *   For each keyword, take most similar library entity as seed
     *
     * Implementation:
     *   1. For each query keyword, compute its embedding via EmbeddingModel
     *   2. Compare against all entity embeddings in the graph (pre-computed during indexing)
     *   3. Take the best matching entity as a seed for that keyword
     *   4. Keep exact name match as a fast-path shortcut
     */
    private List<SeedEntity> getSeedEntities(List<String> queryKeywords) {
        if (queryKeywords == null || queryKeywords.isEmpty() || embeddingModel == null) {
            return Collections.emptyList();
        }

        Map<String, float[]> entityEmbeddings = graph.getAllEntityEmbeddings();
        if (entityEmbeddings.isEmpty()) {
            return Collections.emptyList();
        }

        List<SeedEntity> seeds = new ArrayList<>();
        Set<String> allEntityIds = graph.getAllEntityIds();

        for (String keyword : queryKeywords) {
            if (keyword == null || keyword.length() < 2) continue;

            String lowerKeyword = keyword.toLowerCase();

            // Fast-path: exact name match → similarity = 1.0
            String exactEntityId = "e:" + lowerKeyword;
            if (allEntityIds.contains(exactEntityId)) {
                seeds.add(new SeedEntity(exactEntityId, 1.0, 1));
                continue;
            }

            // Compute keyword embedding
            float[] keywordEmbedding = computeEmbedding(keyword);
            if (keywordEmbedding == null) continue;

            // Compare against all entity embeddings — find best match
            String bestEntityId = null;
            double bestSimilarity = 0.0;

            for (Map.Entry<String, float[]> entry : entityEmbeddings.entrySet()) {
                String entityId = entry.getKey();
                float[] entityEmb = entry.getValue();

                double similarity = cosineSimilarity(keywordEmbedding, entityEmb);
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestEntityId = entityId;
                }
            }

            if (bestEntityId != null && bestSimilarity > 0.3) {
                seeds.add(new SeedEntity(bestEntityId, bestSimilarity, 1));
            }
        }

        // Deduplicate by entityId, keeping highest similarity
        Map<String, SeedEntity> deduped = new LinkedHashMap<>();
        for (SeedEntity seed : seeds) {
            deduped.merge(seed.entityId, seed, (a, b) -> a.similarity > b.similarity ? a : b);
        }

        // Sort by similarity descending, limit to maxSeedEntities
        return deduped.values().stream()
                .sorted(Comparator.comparingDouble((SeedEntity s) -> s.similarity).reversed())
                .limit(config.getMaxSeedEntities())
                .collect(Collectors.toList());
    }

    // ==================== Step 2: Graph Search with Seed Entities ====================

    /**
     * Core graph search: BFS diffusion → passage scoring → PPR.
     */
    private List<TextChunkVO> graphSearchWithSeedEntities(String query, List<SeedEntity> seedEntities,
                                                          float[] queryEmbedding, int topK, float minScore) {
        // 2a: BFS entity score diffusion (uses sentence embeddings vs query embedding)
        Map<String, BfsEntityScore> entityScores = calculateEntityScores(seedEntities, queryEmbedding);
        log.debug("BFS diffusion activated {} entities", entityScores.size());

        // 2b: Calculate passage scores (DPR via pgvector + entity bonus + attribute keyword boost)
        Map<String, Double> passageScores = calculatePassageScores(query, entityScores);

        // 2c: Run PPR with combined reset distribution
        Map<String, Double> entityResetScores = new HashMap<>();
        for (Map.Entry<String, BfsEntityScore> entry : entityScores.entrySet()) {
            entityResetScores.put(entry.getKey(), entry.getValue().score);
        }

        Map<String, Double> pprScores = ppr.runPpr(entityResetScores, passageScores);

        // Convert to TextChunkVO, filter and sort
        return pprScores.entrySet().stream()
                .filter(e -> e.getValue() >= minScore)
                .limit(topK)
                .map(e -> {
                    String paragraphId = e.getKey().startsWith("p:") ? e.getKey().substring(2) : e.getKey();
                    return new TextChunkVO(paragraphId, e.getValue().floatValue());
                })
                .collect(Collectors.toList());
    }

    // ==================== 2a: BFS Entity Score Diffusion ====================

    /**
     * BFS entity score diffusion via entity→sentence→entity path.
     *
     * Per the paper:
     *   Seed entities (tier=1)
     *     → Find all sentences for this entity (filter already used)
     *     → Compute sentence vs query embedding similarity, take Top-K sentences
     *     → For each Top-K sentence, find all entities in it
     *     → New entity score = current entity score × sentence similarity
     *     → If new score < iterationThreshold, prune
     *     → Add to next round (tier=2, 3, ...)
     *
     * Score is multiplicative decay: entities further from seeds get lower scores.
     */
    private Map<String, BfsEntityScore> calculateEntityScores(List<SeedEntity> seedEntities,
                                                               float[] queryEmbedding) {
        Map<String, BfsEntityScore> allScores = new LinkedHashMap<>();
        Set<String> usedSentences = new HashSet<>();

        // Initialize with seed entities (tier=1)
        Queue<BfsEntityScore> queue = new LinkedList<>();
        for (SeedEntity seed : seedEntities) {
            BfsEntityScore bfsScore = new BfsEntityScore(seed.entityId, seed.similarity, 1);
            allScores.put(seed.entityId, bfsScore);
            queue.add(bfsScore);
        }

        // BFS diffusion: maxIterations rounds
        for (int tier = 1; tier <= config.getMaxIterations(); tier++) {
            Queue<BfsEntityScore> nextQueue = new LinkedList<>();

            while (!queue.isEmpty()) {
                BfsEntityScore current = queue.poll();

                if (current.score < config.getIterationThreshold() && tier > 1) {
                    continue; // Prune: score too low to expand
                }

                // Find all sentences containing this entity
                Set<String> sentences = graph.getEntitySentences(current.entityId);
                if (sentences.isEmpty()) continue;

                // Filter out already used sentences
                List<String> candidateSentences = sentences.stream()
                        .filter(s -> !usedSentences.contains(s))
                        .collect(Collectors.toList());

                if (candidateSentences.isEmpty()) continue;

                // Compute sentence vs query embedding similarity, take Top-K
                List<SentenceScore> scoredSentences = new ArrayList<>();
                for (String sentenceId : candidateSentences) {
                    float[] sentenceEmb = graph.getSentenceEmbedding(sentenceId);
                    double similarity;
                    if (sentenceEmb != null && queryEmbedding != null) {
                        similarity = cosineSimilarity(queryEmbedding, sentenceEmb);
                    } else {
                        similarity = 0.5; // Fallback when embeddings unavailable
                    }
                    scoredSentences.add(new SentenceScore(sentenceId, similarity));
                }

                scoredSentences.sort(Comparator.comparingDouble((SentenceScore s) -> s.score).reversed());
                List<SentenceScore> topSentences = scoredSentences.subList(
                        0, Math.min(config.getTopKSentence(), scoredSentences.size()));

                // For each top sentence, find all entities and propagate scores
                for (SentenceScore topSentence : topSentences) {
                    usedSentences.add(topSentence.sentenceId);

                    Set<String> entitiesInSentence = graph.getSentenceEntities(topSentence.sentenceId);
                    for (String newEntityId : entitiesInSentence) {
                        if (allScores.containsKey(newEntityId)) continue; // Already scored

                        // New entity score = parent score × sentence similarity
                        double newScore = current.score * topSentence.score;

                        if (newScore >= config.getIterationThreshold()) {
                            BfsEntityScore newBfsScore = new BfsEntityScore(newEntityId, newScore, tier + 1);
                            allScores.put(newEntityId, newBfsScore);
                            if (tier < config.getMaxIterations()) {
                                nextQueue.add(newBfsScore);
                            }
                        }
                    }
                }
            }

            queue = nextQueue;
            if (queue.isEmpty()) break;
        }

        return allScores;
    }

    // ==================== 2b: Passage Score Calculation ====================

    /**
     * Calculate passage scores from three components:
     *   passageScore = passageRatio × DPR_normalized + log(1 + entityBonus)
     *
     * Then optionally add attribute keyword boost and scale by passageNodeWeight.
     *
     * DPR scores come from pgvector (the existing vector store), not from in-memory
     * paragraph embeddings. This ensures consistency with the passage embeddings
     * already stored during document indexing.
     */
    private Map<String, Double> calculatePassageScores(String query,
                                                        Map<String, BfsEntityScore> entityScores) {
        Set<String> paragraphIds = graph.getAllParagraphIds();
        Map<String, Double> passageScores = new HashMap<>();

        // ① DPR dense retrieval scores from pgvector
        Map<String, Double> dprScores = computeDprScores(query);

        // Min-max normalize DPR scores
        Map<String, Double> normalizedDpr = minMaxNormalize(dprScores);

        // ② Entity bonus per paragraph
        Map<String, Double> entityBonusMap = computeEntityBonus(entityScores);

        // Combine: passageScore = passageRatio × DPR_normalized + log(1 + entityBonus)
        for (String paragraphId : paragraphIds) {
            double dprScore = normalizedDpr.getOrDefault(paragraphId, 0.0);
            double entityBonus = entityBonusMap.getOrDefault(paragraphId, 0.0);

            double score = config.getPassageRatio() * dprScore + Math.log(1 + entityBonus);

            // ③ Attribute keyword boost (optional)
            if (config.isEnableHybridAttributeFallback()) {
                double attrBoost = computeAttributeKeywordBoost(query, paragraphId);
                score += attrBoost;
            }

            // Scale by passageNodeWeight for PPR reset distribution
            score *= config.getPassageNodeWeight();

            if (score > 0) {
                passageScores.put(paragraphId, score);
            }
        }

        return passageScores;
    }

    /**
     * Compute DPR (Dense Passage Retrieval) scores via pgvector.
     *
     * Per the paper: passageEmbeddings × queryEmbedding → cosine similarity
     *
     * Implementation: delegates to VectorStoreImpl.search() which queries pgvector
     * (the same store used during document indexing). This avoids re-computing
     * paragraph embeddings and ensures consistency.
     */
    private Map<String, Double> computeDprScores(String query) {
        Map<String, Double> dprScores = new HashMap<>();

        if (vectorStore == null || searchContext == null || query == null) {
            return dprScores;
        }

        try {
            // Build a search request for pgvector — fetch more candidates than needed
            // so the PPR formula has enough passage scores to work with
            SearchRequest dprRequest = new SearchRequest();
            dprRequest.setQuery(query);
            dprRequest.setKnowledgeIds(searchContext.getKnowledgeIds());
            dprRequest.setExcludeDocumentIds(searchContext.getExcludeDocumentIds());
            dprRequest.setExcludeParagraphIds(searchContext.getExcludeParagraphIds());
            dprRequest.setTopK(DPR_CANDIDATE_COUNT);
            dprRequest.setMinScore(0.0f); // No threshold — we want all candidates

            List<TextChunkVO> results = vectorStore.search(dprRequest);

            for (TextChunkVO result : results) {
                // Map to "p:" prefixed paragraph ID to match graph node IDs
                dprScores.put("p:" + result.getParagraphId(), result.getScore().doubleValue());
            }
        } catch (Exception e) {
            log.warn("Failed to compute DPR scores via pgvector: {}", e.getMessage());
        }

        return dprScores;
    }

    /**
     * Compute entity bonus for each paragraph:
     *   entityBonus += entityScore × log(1 + occurrences_in_passage) / tier
     *   passageScore += log(1 + totalEntityBonus)
     */
    private Map<String, Double> computeEntityBonus(Map<String, BfsEntityScore> entityScores) {
        Map<String, Double> bonusMap = new HashMap<>();

        for (Map.Entry<String, BfsEntityScore> entry : entityScores.entrySet()) {
            String entityId = entry.getKey();
            BfsEntityScore bfsScore = entry.getValue();

            Set<String> paragraphs = graph.getEntityParagraphs(entityId);
            for (String paragraphId : paragraphs) {
                int occurrences = graph.getEntityOccurrenceInParagraph(entityId, paragraphId);
                double bonus = bfsScore.score * Math.log(1 + occurrences) / bfsScore.tier;
                bonusMap.merge(paragraphId, bonus, Double::sum);
            }
        }

        // Apply log(1 + totalBonus) transformation
        bonusMap.replaceAll((k, v) -> Math.log(1 + v));

        return bonusMap;
    }

    /**
     * Compute attribute keyword boost for property-based queries.
     */
    private double computeAttributeKeywordBoost(String query, String paragraphId) {
        String lowerQuery = query.toLowerCase();

        List<String> attributeKeywords = new ArrayList<>();
        for (String kw : LinearRagConfig.ATTRIBUTE_KEYWORDS_EN) {
            if (lowerQuery.contains(kw)) attributeKeywords.add(kw);
        }
        for (String kw : LinearRagConfig.ATTRIBUTE_KEYWORDS_ZH) {
            if (lowerQuery.contains(kw)) attributeKeywords.add(kw);
        }

        if (attributeKeywords.isEmpty()) return 0.0;

        GraphNode paraNode = graph.getNode(paragraphId);
        if (paraNode == null || paraNode.getContent() == null) return 0.0;

        String lowerContent = paraNode.getContent().toLowerCase();
        long overlapCount = attributeKeywords.stream().filter(lowerContent::contains).count();

        return config.getAttributeKeywordBoost() * Math.log(1 + overlapCount);
    }

    // ==================== Step 3: Fallback Dense Retrieval ====================

    /**
     * Fallback: when no seed entities are found, use pure dense passage retrieval via pgvector.
     * Delegates to VectorStoreImpl.search() which queries pgvector for cosine similarity.
     */
    private List<TextChunkVO> densePassageRetrieval(String query, int topK, float minScore) {
        if (vectorStore == null || searchContext == null) {
            return Collections.emptyList();
        }

        try {
            SearchRequest fallbackRequest = new SearchRequest();
            fallbackRequest.setQuery(query);
            fallbackRequest.setKnowledgeIds(searchContext.getKnowledgeIds());
            fallbackRequest.setExcludeDocumentIds(searchContext.getExcludeDocumentIds());
            fallbackRequest.setExcludeParagraphIds(searchContext.getExcludeParagraphIds());
            fallbackRequest.setTopK(topK);
            fallbackRequest.setMinScore(minScore);

            return vectorStore.search(fallbackRequest);
        } catch (Exception e) {
            log.warn("Dense passage retrieval fallback failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Compute embedding for a single text string using the EmbeddingModel.
     */
    private float[] computeEmbedding(String text) {
        if (embeddingModel == null || text == null || text.isBlank()) return null;
        try {
            Response<Embedding> response = embeddingModel.embed(TextSegment.from(text));
            return response.content().vector();
        } catch (Exception e) {
            log.debug("Failed to compute embedding for text: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Cosine similarity between two vectors.
     */
    static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) return 0.0;

        return dotProduct / denominator;
    }

    /**
     * Min-max normalization to [0, 1].
     */
    static Map<String, Double> minMaxNormalize(Map<String, Double> scores) {
        if (scores.isEmpty()) return scores;

        double min = scores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double range = max - min;

        if (range == 0.0) {
            Map<String, Double> uniform = new HashMap<>();
            for (String key : scores.keySet()) uniform.put(key, 0.5);
            return uniform;
        }

        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            normalized.put(entry.getKey(), (entry.getValue() - min) / range);
        }
        return normalized;
    }

    // ==================== Inner Classes ====================

    /** Seed entity from initial matching. */
    record SeedEntity(String entityId, double similarity, int tier) {
    }

    /** BFS-activated entity with score and tier. */
    record BfsEntityScore(String entityId, double score, int tier) {
    }

    /** Scored sentence during BFS. */
    record SentenceScore(String sentenceId, double score) {
    }
}
