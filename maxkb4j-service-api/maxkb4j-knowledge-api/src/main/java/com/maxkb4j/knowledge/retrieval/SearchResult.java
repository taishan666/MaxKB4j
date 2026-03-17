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
    private float maxScore;

    /**
     * Minimum similarity score in results
     */
    private float minScore;

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
                .maxScore(0.0f)
                .minScore(0.0f)
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

        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;

        for (TextChunkVO chunk : chunks) {
            float score = chunk.getScore() != null ? chunk.getScore() : 0.0f;
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