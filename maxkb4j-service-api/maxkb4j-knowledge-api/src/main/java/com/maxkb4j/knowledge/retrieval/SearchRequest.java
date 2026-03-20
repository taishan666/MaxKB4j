package com.maxkb4j.knowledge.retrieval;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Search request for vector and full-text search
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest {

    /**
     * Search query text
     */
    private String query;

    /**
     * Knowledge base IDs to search within
     */
    private List<String> knowledgeIds;
    /**
     * Paragraph IDs to exclude from results
     */
    private List<String> excludeDocumentIds;
    /**
     * Paragraph IDs to exclude from results
     */
    private List<String> excludeParagraphIds;

    /**
     * Search mode: VECTOR, FULL_TEXT, or HYBRID
     */
    private SearchMode mode = SearchMode.VECTOR;

    /**
     * Maximum number of results to return
     */
    private int topK = 5;

    /**
     * Minimum similarity score threshold
     */
    private float minScore = 0.0f;

    /**
     * Page number for pagination (1-based)
     */
    private int page = 1;

    /**
     * Page size for pagination
     */
    private int pageSize = 10;

    /**
     * Additional filters for extended filtering capabilities
     */
    private Map<String, Object> filters;

}