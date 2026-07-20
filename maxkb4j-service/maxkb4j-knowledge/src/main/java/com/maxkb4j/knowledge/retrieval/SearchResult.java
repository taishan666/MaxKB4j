package com.maxkb4j.knowledge.retrieval;

import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Search result containing matched text chunks and metadata
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult {

    /**
     * Matched text chunks
     */
    private List<TextChunkVO> chunks;

    /**
     * Total number of matching results (before pagination)
     */
    private long total;

    /**
     * Maximum similarity score in results
     */
    private double maxScore;

    /**
     * Minimum similarity score in results
     */
    private double minScore;

    /**
     * Time taken for search in milliseconds
     */
    private long tookMs;

    /**
     * Create an empty search result
     */
    public static SearchResult empty() {
        return SearchResult.builder()
                .chunks(Collections.emptyList())
                .total(0)
                .maxScore(0.0d)
                .minScore(0.0d)
                .tookMs(0)
                .build();
    }

    /**
     * Create a search result from a list of chunks
     */
    public static SearchResult of(List<TextChunkVO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return empty();
        }

        // 注意：不能用 Double.MIN_VALUE 作为最大值初值——它是最小正数(4.9E-324)，
        // 当所有得分 <= 0 时会得到错误的 max。用负/正无穷作为上下界最稳健。
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        for (TextChunkVO chunk : chunks) {
            double score = chunk.getScore() != null ? chunk.getScore() : 0d;
            if (score > max) max = score;
            if (score < min) min = score;
        }

        return SearchResult.builder()
                .chunks(chunks)
                .total(chunks.size())
                .maxScore(max)
                .minScore(min)
                .build();
    }

    /**
     * Create a search result with timing information
     */
    public static SearchResult of(List<TextChunkVO> chunks, long tookMs) {
        SearchResult result = of(chunks);
        result.setTookMs(tookMs);
        return result;
    }
}
