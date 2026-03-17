package com.maxkb4j.knowledge.retrieval;

import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reciprocal Rank Fusion (RRF) algorithm implementation for hybrid search
 *
 * RRF formula: score(d) = sum(1 / (k + rank(d))) for each ranking list
 * where k is a constant (typically 60) that reduces the impact of high rankings
 */
@Setter
@Component
public class RRFFusion {

    /**
     * Default RRF constant (recommended value from research papers)
     */
    private static final int DEFAULT_K = 60;

    /**
     * -- SETTER --
     *  Set the RRF constant k
     */
    @Value("${search.rrf.k:60}")
    private int k = DEFAULT_K;

    /**
     * Perform RRF fusion on multiple result lists
     *
     * @param resultLists list of search result lists to fuse
     * @param topK maximum number of results to return
     * @return fused and sorted list of text chunks
     */
    public List<TextChunkVO> fuse(List<List<TextChunkVO>> resultLists, int topK) {
        return fuse(resultLists, topK, k);
    }

    /**
     * Perform RRF fusion with custom k parameter
     *
     * @param resultLists list of search result lists to fuse
     * @param topK maximum number of results to return
     * @param k RRF constant
     * @return fused and sorted list of text chunks
     */
    public List<TextChunkVO> fuse(List<List<TextChunkVO>> resultLists, int topK, int k) {
        if (resultLists == null || resultLists.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate RRF scores for each unique paragraph
        Map<String, Double> rrfScores = new HashMap<>();

        for (List<TextChunkVO> resultList : resultLists) {
            if (resultList == null) continue;

            for (int rank = 0; rank < resultList.size(); rank++) {
                TextChunkVO chunk = resultList.get(rank);
                String paragraphId = chunk.getParagraphId();

                // RRF score contribution: 1 / (k + rank + 1)
                double contribution = 1.0 / (k + rank + 1);
                rrfScores.merge(paragraphId, contribution, Double::sum);
            }
        }

        // Sort by RRF score and return top K results
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> new TextChunkVO(entry.getKey(), entry.getValue().floatValue()))
                .collect(Collectors.toList());
    }

    /**
     * Perform weighted RRF fusion
     * Allows assigning different weights to different result lists
     *
     * @param resultLists list of search result lists to fuse
     * @param weights weights for each result list (must match size of resultLists)
     * @param topK maximum number of results to return
     * @return fused and sorted list of text chunks
     */
    public List<TextChunkVO> weightedFuse(List<List<TextChunkVO>> resultLists,
                                          List<Double> weights,
                                          int topK) {
        if (resultLists == null || resultLists.isEmpty()) {
            return Collections.emptyList();
        }

        if (weights == null || weights.size() != resultLists.size()) {
            // If weights don't match, use uniform weights
            return fuse(resultLists, topK);
        }

        Map<String, Double> rrfScores = new HashMap<>();

        for (int listIndex = 0; listIndex < resultLists.size(); listIndex++) {
            List<TextChunkVO> resultList = resultLists.get(listIndex);
            double weight = weights.get(listIndex);

            if (resultList == null) continue;

            for (int rank = 0; rank < resultList.size(); rank++) {
                TextChunkVO chunk = resultList.get(rank);
                String paragraphId = chunk.getParagraphId();

                // Weighted RRF score contribution
                double contribution = weight / (k + rank + 1);
                rrfScores.merge(paragraphId, contribution, Double::sum);
            }
        }

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> new TextChunkVO(entry.getKey(), entry.getValue().floatValue()))
                .collect(Collectors.toList());
    }

}