package com.maxkb4j.knowledge.retrieval;

import dev.langchain4j.data.embedding.Embedding;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
     * Document IDs to exclude from results
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
    private double minScore = 0D;

    /**
     * 请求级查询向量缓存：query 首次向量化后回填，供同一次检索的段落/问题双路召回复用，
     * 避免对同一 query 重复调用 embedding 模型。
     * <p>非调用方入参。仅在请求生命周期内有效——同请求的 knowledgeIds（即 embedding 模型）
     * 与 query 不变，缓存才成立；请求对象每次检索新建，勿跨请求复用。</p>
     */
    private Embedding queryEmbedding;

}