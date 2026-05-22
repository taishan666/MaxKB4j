package com.maxkb4j.knowledge.linearrag;

import com.maxkb4j.knowledge.linearrag.model.TriGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Personalized PageRank (PPR) implementation for LinearRAG.
 * Given a set of activated seed entity nodes, PPR propagates relevance scores
 * through the Tri-Graph to rank paragraph nodes by global importance.
 * Algorithm:
 *   p = alpha * W * p + (1 - alpha) * v
 *   where:
 *     p     = score vector (per node)
 *     W     = column-normalized adjacency matrix
 *     v     = personalization (seed) vector
 *     alpha = damping factor (typically 0.85)
 */
public class PersonalizedPageRank {

    private final TriGraph graph;
    private final double dampingFactor;
    private final int maxIterations;
    private final double convergenceThreshold;

    public PersonalizedPageRank(TriGraph graph) {
        this(graph, 0.85, 50, 1e-6);
    }

    public PersonalizedPageRank(TriGraph graph, double dampingFactor, int maxIterations, double convergenceThreshold) {
        this.graph = graph;
        this.dampingFactor = dampingFactor;
        this.maxIterations = maxIterations;
        this.convergenceThreshold = convergenceThreshold;
    }

    /**
     * Compute PPR scores given a set of seed nodes with initial scores.
     *
     * @param seedScores map of node ID -> initial activation score
     * @return map of paragraph ID -> PPR relevance score, sorted descending
     */
    public Map<String, Double> compute(Map<String, Double> seedScores) {
        if (seedScores == null || seedScores.isEmpty()) {
            return Collections.emptyMap();
        }

        // Collect all node IDs
        Set<String> allNodeIds = new HashSet<>();
        allNodeIds.addAll(graph.getAllEntityIds());
        allNodeIds.addAll(graph.getAllSentenceIds());
        allNodeIds.addAll(graph.getAllParagraphIds());

        if (allNodeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build node index mapping for efficient computation
        List<String> nodeIndex = new ArrayList<>(allNodeIds);
        Map<String, Integer> nodeIdToIndex = new HashMap<>();
        for (int i = 0; i < nodeIndex.size(); i++) {
            nodeIdToIndex.put(nodeIndex.get(i), i);
        }
        int n = nodeIndex.size();

        // Initialize personalization vector v
        double[] v = new double[n];
        double totalSeedScore = seedScores.values().stream().mapToDouble(Double::doubleValue).sum();
        for (Map.Entry<String, Double> entry : seedScores.entrySet()) {
            Integer idx = nodeIdToIndex.get(entry.getKey());
            if (idx != null) {
                v[idx] = entry.getValue() / totalSeedScore; // normalize
            }
        }

        // Initialize score vector p = v
        double[] p = Arrays.copyOf(v, n);

        // Compute out-degree for each node (for normalization)
        double[] outDegree = new double[n];
        for (int i = 0; i < n; i++) {
            String nodeId = nodeIndex.get(i);
            Set<String> neighbors = graph.getNeighbors(nodeId);
            outDegree[i] = Math.max(neighbors.size(), 1);
        }

        // Iterative PPR computation
        for (int iter = 0; iter < maxIterations; iter++) {
            double[] newP = new double[n];

            // Propagate scores through edges
            for (int i = 0; i < n; i++) {
                String nodeId = nodeIndex.get(i);
                Set<String> neighbors = graph.getNeighbors(nodeId);
                double propagatedScore = 0.0;

                for (String neighborId : neighbors) {
                    Integer j = nodeIdToIndex.get(neighborId);
                    if (j != null) {
                        propagatedScore += p[j] / outDegree[j];
                    }
                }

                newP[i] = dampingFactor * propagatedScore + (1 - dampingFactor) * v[i];
            }

            // Check convergence
            double diff = 0.0;
            for (int i = 0; i < n; i++) {
                diff += Math.abs(newP[i] - p[i]);
            }

            p = newP;

            if (diff < convergenceThreshold) {
                break;
            }
        }

        // Extract paragraph scores
        Map<String, Double> paragraphScores = new HashMap<>();
        for (String paragraphId : graph.getAllParagraphIds()) {
            Integer idx = nodeIdToIndex.get(paragraphId);
            if (idx != null) {
                paragraphScores.put(paragraphId, p[idx]);
            }
        }

        // Sort by score descending
        return paragraphScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Simplified PPR that directly computes paragraph scores from seed entities,
     * using graph structure for score propagation without full matrix operations.
     * More efficient for large graphs.
     */
    public Map<String, Double> computeParagraphScores(Map<String, Double> entitySeedScores) {
        if (entitySeedScores == null || entitySeedScores.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> paragraphScores = new HashMap<>();

        // Phase 1: Direct entity -> paragraph propagation
        for (Map.Entry<String, Double> entry : entitySeedScores.entrySet()) {
            String entityId = entry.getKey();
            double score = entry.getValue();

            Set<String> paragraphs = graph.getEntityParagraphs(entityId);
            if (!paragraphs.isEmpty()) {
                double perParagraphScore = score / paragraphs.size();
                for (String paraId : paragraphs) {
                    paragraphScores.merge(paraId, perParagraphScore, Double::sum);
                }
            }
        }

        // Phase 2: Second-hop propagation via co-occurring entities
        Map<String, Double> expandedScores = new HashMap<>(entitySeedScores);
        for (Map.Entry<String, Double> entry : entitySeedScores.entrySet()) {
            String entityId = entry.getKey();
            double score = entry.getValue();

            Set<String> coEntities = graph.getCoOccurringEntities(entityId);
            double hopScore = score * 0.3; // decay factor for second hop
            for (String coEntityId : coEntities) {
                if (!expandedScores.containsKey(coEntityId)) {
                    expandedScores.put(coEntityId, hopScore);
                    // Propagate to co-entity's paragraphs
                    Set<String> coParagraphs = graph.getEntityParagraphs(coEntityId);
                    if (!coParagraphs.isEmpty()) {
                        double perParaScore = hopScore / coParagraphs.size();
                        for (String paraId : coParagraphs) {
                            paragraphScores.merge(paraId, perParaScore, Double::sum);
                        }
                    }
                }
            }
        }

        // Phase 3: Normalize scores to [0, 1]
        double maxScore = paragraphScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);

        if (maxScore > 0) {
            paragraphScores.replaceAll((k, v) -> v / maxScore);
        }

        // Sort descending
        return paragraphScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
