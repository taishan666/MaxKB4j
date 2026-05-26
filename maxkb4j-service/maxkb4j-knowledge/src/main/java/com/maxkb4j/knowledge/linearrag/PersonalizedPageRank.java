package com.maxkb4j.knowledge.linearrag;

import com.maxkb4j.knowledge.linearrag.model.TriGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Personalized PageRank (PPR) implementation for LinearRAG per the paper specification.
 *
 * Given a combined reset distribution (BFS entity scores + passage DPR scores),
 * PPR propagates relevance through the weighted graph to rank paragraph nodes.
 *
 * Algorithm:
 *   p = (1 - d) * reset + d * M * p
 *
 * Where:
 *   d       = damping factor (0.5, smaller than standard 0.85, more reliance on reset)
 *   M       = column-normalized weighted adjacency matrix
 *   reset   = personalization vector from entity scores + passage scores
 *   maxIter = 100, tolerance = 1e-6
 *
 * The graph only contains entity↔paragraph and paragraph↔paragraph weighted edges.
 * Sentence nodes are NOT part of the PPR graph (they are used in BFS only).
 */
public class PersonalizedPageRank {

    private final TriGraph graph;
    private final LinearRagConfig config;

    public PersonalizedPageRank(TriGraph graph, LinearRagConfig config) {
        this.graph = graph;
        this.config = config;
    }

    /**
     * Run PPR with a combined reset distribution from entity scores and passage scores.
     * <p>
     * Follows the Python LinearRAG implementation using sparse adjacency representation:
     * <pre>
     *   reset_prob = np.where(np.isnan(node_weights) | (node_weights < 0), 0, node_weights)
     *   pagerank_scores = graph.personalized_pagerank(reset=reset_prob, damping=damping)
     * </pre>
     * <p>
     * Uses sparse adjacency lists instead of a dense n×n matrix for efficiency.
     * Matrix-vector multiplication only iterates over actual edges: O(edges) per iteration.
     *
     * @param entityScores  BFS-computed entity scores (entityId -> score)
     * @param passageScores DPR-computed passage scores (paragraphId -> score, already scaled by passageNodeWeight)
     * @return map of paragraph ID -> PPR relevance score, sorted descending
     */
    public Map<String, Double> runPpr(Map<String, Double> entityScores, Map<String, Double> passageScores) {
        if ((entityScores == null || entityScores.isEmpty())
                && (passageScores == null || passageScores.isEmpty())) {
            return Collections.emptyMap();
        }

        // Collect all node IDs that participate in PPR (entities + paragraphs only, not sentences)
        Set<String> pprNodeIds = new LinkedHashSet<>();
        pprNodeIds.addAll(graph.getAllEntityIds());
        pprNodeIds.addAll(graph.getAllParagraphIds());

        if (pprNodeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build node index mapping
        List<String> nodeIndex = new ArrayList<>(pprNodeIds);
        Map<String, Integer> nodeIdToIndex = new HashMap<>();
        for (int i = 0; i < nodeIndex.size(); i++) {
            nodeIdToIndex.put(nodeIndex.get(i), i);
        }
        int n = nodeIndex.size();

        // Build reset distribution (personalization vector) — corresponds to node_weights in Python
        double[] reset = buildResetDistribution(nodeIndex, nodeIdToIndex, entityScores, passageScores);

        // Clean reset: replace NaN and negative values with 0 (Python: np.where(isnan | <0, 0, weights))
        for (int i = 0; i < n; i++) {
            if (Double.isNaN(reset[i]) || reset[i] < 0) {
                reset[i] = 0.0;
            }
        }

        // Normalize reset to sum to 1 (probability distribution)
        double resetSum = 0;
        for (double v : reset) resetSum += v;
        if (resetSum > 0) {
            for (int i = 0; i < n; i++) reset[i] /= resetSum;
        } else {
            // Uniform reset if no scores
            for (int i = 0; i < n; i++) reset[i] = 1.0 / n;
        }

        // Build sparse column-normalized transition structure (replaces dense n×n matrix)
        // For each source node j: flat arrays of (targetIndex[], normalizedWeight[])
        // This is equivalent to the column-normalized adjacency matrix M, but only stores non-zero entries
        int[][] outNeighborIdx = new int[n][];
        double[][] outNeighborWt = new double[n][];
        for (int j = 0; j < n; j++) {
            String nodeId = nodeIndex.get(j);
            Map<String, Double> neighbors = graph.getWeightedNeighbors(nodeId);

            // First pass: calculate out-weight for column normalization
            double outWeight = 0;
            int validCount = 0;
            for (Map.Entry<String, Double> entry : neighbors.entrySet()) {
                if (nodeIdToIndex.containsKey(entry.getKey())) {
                    outWeight += entry.getValue();
                    validCount++;
                }
            }

            // Second pass: build flat arrays of normalized weights
            outNeighborIdx[j] = new int[validCount];
            outNeighborWt[j] = new double[validCount];
            int pos = 0;
            if (outWeight > 0) {
                for (Map.Entry<String, Double> entry : neighbors.entrySet()) {
                    Integer i = nodeIdToIndex.get(entry.getKey());
                    if (i != null) {
                        outNeighborIdx[j][pos] = i;
                        outNeighborWt[j][pos] = entry.getValue() / outWeight;
                        pos++;
                    }
                }
            }
        }

        // Power iteration: p = (1-d)*reset + d*M*p
        // Equivalent to igraph's personalized_pagerank with damping factor
        double d = config.getDamping();
        double[] p = Arrays.copyOf(reset, n);

        for (int iter = 0; iter < config.getPprMaxIter(); iter++) {
            double[] newP = new double[n];

            // Sparse M * p: only iterate over actual edges instead of full n×n matrix
            for (int j = 0; j < n; j++) {
                if (p[j] == 0.0) continue; // skip zero-probability nodes
                int[] idxArr = outNeighborIdx[j];
                double[] wtArr = outNeighborWt[j];
                double pj = p[j];
                for (int k = 0; k < idxArr.length; k++) {
                    newP[idxArr[k]] += wtArr[k] * pj;
                }
            }

            // Apply damping: p = (1-d)*reset + d*M*p
            for (int i = 0; i < n; i++) {
                newP[i] = (1 - d) * reset[i] + d * newP[i];
            }

            // Check convergence (L1 norm)
            double diff = 0.0;
            for (int i = 0; i < n; i++) {
                diff += Math.abs(newP[i] - p[i]);
            }

            p = newP;

            if (diff < config.getPprTolerance()) {
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
     * Build the reset distribution from entity scores and passage scores.
     * Entity scores come from BFS diffusion.
     * Passage scores come from DPR + entity bonus, scaled by passageNodeWeight.
     */
    private double[] buildResetDistribution(List<String> nodeIndex, Map<String, Integer> nodeIdToIndex,
                                            Map<String, Double> entityScores, Map<String, Double> passageScores) {
        int n = nodeIndex.size();
        double[] reset = new double[n];

        // Add entity scores to reset
        if (entityScores != null) {
            for (Map.Entry<String, Double> entry : entityScores.entrySet()) {
                Integer idx = nodeIdToIndex.get(entry.getKey());
                if (idx != null) {
                    reset[idx] += entry.getValue();
                }
            }
        }

        // Add passage scores to reset (already scaled by passageNodeWeight)
        if (passageScores != null) {
            for (Map.Entry<String, Double> entry : passageScores.entrySet()) {
                Integer idx = nodeIdToIndex.get(entry.getKey());
                if (idx != null) {
                    reset[idx] += entry.getValue();
                }
            }
        }

        return reset;
    }

    // buildTransitionMatrix removed — replaced by sparse adjacency lists built inline in runPpr
}
