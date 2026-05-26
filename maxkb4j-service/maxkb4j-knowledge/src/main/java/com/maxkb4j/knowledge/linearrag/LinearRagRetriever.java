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
        List<NodeScore> seedEntities = getSeedEntities(queryKeywords);

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
    private List<NodeScore> getSeedEntities(List<String> queryKeywords) {
        if (queryKeywords == null || queryKeywords.isEmpty() || embeddingModel == null) {
            return Collections.emptyList();
        }

        Map<String, float[]> entityEmbeddings = graph.getAllEntityEmbeddings();
        if (entityEmbeddings.isEmpty()) {
            return Collections.emptyList();
        }

        List<NodeScore> seeds = new ArrayList<>();
        Set<String> allEntityIds = graph.getAllEntityIds();

        for (String keyword : queryKeywords) {
            if (keyword == null || keyword.length() < 2) continue;

            String lowerKeyword = keyword.toLowerCase();

            // Fast-path: exact name match → similarity = 1.0
            String exactEntityId = "e:" + lowerKeyword;
            if (allEntityIds.contains(exactEntityId)) {
                seeds.add(new NodeScore(exactEntityId, 1.0));
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
                seeds.add(new NodeScore(bestEntityId, bestSimilarity));
            }
        }

        // Deduplicate by entityId, keeping highest similarity
        Map<String, NodeScore> deduped = new LinkedHashMap<>();
        for (NodeScore seed : seeds) {
            deduped.merge(seed.nodeId, seed, (a, b) -> a.score > b.score ? a : b);
        }

        // Sort by similarity descending, limit to maxSeedEntities
        return deduped.values().stream()
                .sorted(Comparator.comparingDouble((NodeScore s) -> s.score).reversed())
                .collect(Collectors.toList());
    }

    // ==================== Step 2: Graph Search with Seed Entities ====================

    /**
     * Core graph search: BFS diffusion → passage scoring → PPR.
     */
    private List<TextChunkVO> graphSearchWithSeedEntities(String query, List<NodeScore> seedEntities, float[] queryEmbedding, int topK, float minScore) {
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
    private Map<String, BfsEntityScore> calculateEntityScores(List<NodeScore> seedEntities, float[] queryEmbedding) {
        Map<String, BfsEntityScore> allScores = new LinkedHashMap<>();
        Set<String> usedSentences = new HashSet<>();

        // 使用队列进行 BFS
        Queue<BfsEntityScore> queue = new LinkedList<>();
        for (NodeScore seed : seedEntities) {
            // 注意：这里建议也加一个初始阈值判断，和 Python 逻辑保持一致
            if (seed.score < config.getIterationThreshold()) continue;

            BfsEntityScore bfsScore = new BfsEntityScore(seed.nodeId, seed.score, 1);
            allScores.put(seed.nodeId, bfsScore);
            queue.add(bfsScore);
        }

        // BFS 扩散
        for (int tier = 1; tier <= config.getMaxIterations(); tier++) {
            Queue<BfsEntityScore> nextQueue = new LinkedList<>();

            while (!queue.isEmpty()) {
                BfsEntityScore current = queue.poll();

                // 【修正1】去掉 tier > 1 的限制，严格对齐 Python 的剪枝逻辑
                if (current.score < config.getIterationThreshold()) {
                    continue;
                }

                Set<String> sentences = graph.getEntitySentences(current.entityId);
                if (sentences == null || sentences.isEmpty()) continue;

                // 过滤已使用的句子
                List<String> candidateSentences = sentences.stream()
                        .filter(s -> !usedSentences.contains(s))
                        .toList();

                if (candidateSentences.isEmpty()) continue;

                // 计算句子相似度并取 Top-K
                List<NodeScore> scoredSentences = new ArrayList<>();
                for (String sentenceId : candidateSentences) {
                    float[] sentenceEmb = graph.getSentenceEmbedding(sentenceId);
                    double similarity;
                    if (sentenceEmb != null && queryEmbedding != null) {
                        // 【修正3】如果 Python 用的是 np.dot，这里建议用 dotProduct 而不是 cosine
                        // 如果你的向量已经 L2 normalized，那 cosine 和 dot 是一样的
                        similarity = dotProduct(queryEmbedding, sentenceEmb);
                    } else {
                        similarity = 0.0; // 建议 fallback 为 0，避免引入噪声
                    }
                    scoredSentences.add(new NodeScore(sentenceId, similarity));
                }

                // 降序排序并截取 Top-K
                scoredSentences.sort(Comparator.comparingDouble((NodeScore s) -> s.score).reversed());
                int topK = Math.min(config.getTopKSentence(), scoredSentences.size());
                List<NodeScore> topSentences = scoredSentences.subList(0, topK);

                // 扩展新实体
                for (NodeScore topSentence : topSentences) {
                    usedSentences.add(topSentence.nodeId);

                    Set<String> entitiesInSentence = graph.getSentenceEntities(topSentence.nodeId);
                    if (entitiesInSentence == null) continue;

                    for (String newEntityId : entitiesInSentence) {
                        // 计算新分数：父实体分数 * 句子相似度
                        double newScore = current.score * topSentence.score;

                        if (newScore >= config.getIterationThreshold()) {
                            // 【修正2】处理得分累加逻辑
                            BfsEntityScore existingScore = allScores.get(newEntityId);
                            double finalScore = newScore;
                            int finalTier = tier + 1;

                            if (existingScore != null) {
                                // 如果实体已存在，累加分数（对齐 Python 的 entity_weights +=）
                                // 注意：Python 中 new_entities 是同层覆盖，但全局 weights 是累加。
                                // 这里我们选择更新分数并保留较小的 tier（最早发现的层级通常更相关）
                                finalScore = existingScore.score + newScore;
                                finalTier = Math.min(existingScore.tier, tier + 1);
                            }

                            BfsEntityScore updatedScore = new BfsEntityScore(newEntityId, finalScore, finalTier);
                            allScores.put(newEntityId, updatedScore);

                            // 只有当该实体是“新发现”或者“层级允许继续扩散”时才加入下一轮队列
                            // 这里简单处理：只要没在队列里处理过（可以通过层级判断，或者允许重复入队但依靠 usedSentences 剪枝）
                            // 为了严格对齐 Python（Python 只要分数够就放入 new_entities 也就是下一轮的 current_entities）：
                            if (tier < config.getMaxIterations()) {
                                // 注意：如果允许分数累加，可能需要重新评估是否将其再次加入队列进行扩散
                                // 但通常 BFS 中一个节点只作为扩展源一次。
                                // 如果完全对齐 Python：Python 的 new_entities 是 map，同层级后面的会覆盖前面的。
                                // 这里简化逻辑：只有第一次发现（或者分数显著更新）时加入 nextQueue
                                if (existingScore == null) {
                                    nextQueue.add(updatedScore);
                                }
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

    // 简单的点积工具方法
    private double dotProduct(float[] a, float[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    // ==================== 2b: Passage Score Calculation ====================

    /**
     * Calculate passage scores following the Python LinearRAG implementation:
     * <pre>
     *   # Step 1: Entity bonus per paragraph (with log per entity), then min-max normalize
     *   for entity in actived_entities:
     *       for passage in entity_passages:
     *           passage_scores[passage] += entity_weight * log(1 + occurrences)
     *   normalized_passage_scores = min_max_normalize(passage_scores)
     *
     *   # Step 2: DPR cosine similarity, then min-max normalize
     *   dpr_scores = dense_passage_retrieval(question_embedding)
     *   normalized_dpr = min_max_normalize(dpr_scores)
     *
     *   # Step 3: Combine and scale
     *   final_scores[passage] = (passageRatio * dpr_norm + entity_bonus_norm) * passageNodeWeight
     * </pre>
     */
    private Map<String, Double> calculatePassageScores(String query, Map<String, BfsEntityScore> entityScores) {
        Set<String> paragraphIds = graph.getAllParagraphIds();

        // ① Entity bonus per paragraph: score × log(1 + occurrences), then min-max normalize
        Map<String, Double> entityBonusRaw = computeEntityBonus(entityScores);
        Map<String, Double> normalizedEntityBonus = minMaxNormalize(entityBonusRaw);

        // ② DPR dense retrieval scores from pgvector, then min-max normalize
        Map<String, Double> dprScores = computeDprScores(query);
        Map<String, Double> normalizedDpr = minMaxNormalize(dprScores);

        // ③ Combine: (passageRatio × DPR_normalized + entityBonus_normalized) × passageNodeWeight
        Map<String, Double> passageScores = new HashMap<>();
        double passageRatio = config.getPassageRatio();
        double passageNodeWeight = config.getPassageNodeWeight();

        for (String paragraphId : paragraphIds) {
            double dprNorm = normalizedDpr.getOrDefault(paragraphId, 0.0);
            double entityBonusNorm = normalizedEntityBonus.getOrDefault(paragraphId, 0.0);

            double score = (passageRatio * dprNorm + entityBonusNorm) * passageNodeWeight;

            // Attribute keyword boost (optional)
            if (config.isEnableHybridAttributeFallback()) {
                double attrBoost = computeAttributeKeywordBoost(query, paragraphId);
                score += attrBoost * passageNodeWeight;
            }

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
     * Compute raw entity bonus for each paragraph (no final log transform).
     * <p>
     * Per the Python implementation:
     * <pre>
     *   for entity in actived_entities:
     *       for passage in entity_passages:
     *           passage_scores[passage] += entity_weight * log(1 + occurrences)
     * </pre>
     * The log(1+occurrences) is applied per-entity, and the raw accumulated bonus is returned.
     * Min-max normalization is applied afterwards in {@link #calculatePassageScores}.
     */
    private Map<String, Double> computeEntityBonus(Map<String, BfsEntityScore> entityScores) {
        Map<String, Double> bonusMap = new HashMap<>();

        for (Map.Entry<String, BfsEntityScore> entry : entityScores.entrySet()) {
            String entityId = entry.getKey();
            BfsEntityScore bfsScore = entry.getValue();

            Set<String> paragraphs = graph.getEntityParagraphs(entityId);
            for (String paragraphId : paragraphs) {
                int occurrences = graph.getEntityOccurrenceInParagraph(entityId, paragraphId);
                // Per Python: entity_weight * log(1 + count)
                double bonus = bfsScore.score * Math.log(1 + occurrences);
                bonusMap.merge(paragraphId, bonus, Double::sum);
            }
        }

        // Return raw accumulated bonus (no final log transform — Python doesn't do it)
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
    /** BFS-activated entity with score and tier. */
    record BfsEntityScore(String entityId, double score, int tier) {}

    /** Scored sentence during BFS. */
    record NodeScore(String nodeId, double score) { }
}
