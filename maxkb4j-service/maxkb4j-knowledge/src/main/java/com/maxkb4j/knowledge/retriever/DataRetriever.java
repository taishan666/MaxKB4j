package com.maxkb4j.knowledge.retriever;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.consts.SearchType;
import com.maxkb4j.knowledge.retrieval.SearchMode;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Unified data retriever that supports multiple search modes
 */
@Slf4j
@Component
public class DataRetriever {

    private final IDataStore vectorStore;
    private final IDataStore fullTextStore;
    private final IDataStore compositeStore;
    private final IDocumentInternalService documentService;
    private final SearchOrchestrator searchOrchestrator;

    public DataRetriever(@Qualifier("vectorStore") IDataStore vectorStore,
                         @Qualifier("fullTextStore") IDataStore fullTextStore,
                         @Qualifier("compositeStore") IDataStore compositeStore,
                         IDocumentInternalService documentService,
                         SearchOrchestrator searchOrchestrator) {
        this.vectorStore = vectorStore;
        this.fullTextStore = fullTextStore;
        this.compositeStore = compositeStore;
        this.documentService = documentService;
        this.searchOrchestrator = searchOrchestrator;
    }

    public List<TextChunkVO> search(List<String> knowledgeIds, List<String> excludeParagraphIds,
                                    String keyword, int maxResults, float minScore, String searchMode) {
        SearchMode mode = resolveMode(searchMode);
        SearchRequest request = new SearchRequest();
        request.setKnowledgeIds(knowledgeIds);
        request.setExcludeParagraphIds(excludeParagraphIds);
        request.setQuery(keyword);
        request.setTopK(maxResults);
        request.setMinScore(minScore);
        request.setMode(mode);
        List<String> excludeDocIds = documentService.getNoActiveDocIds(knowledgeIds);
        if (CollectionUtils.isNotEmpty(excludeDocIds)) {
            request.setExcludeDocumentIds(excludeDocIds);
        }
        return searchOrchestrator.search(getStore(mode), request);
    }

    /**
     * 解析检索模式字符串为枚举；空值或未知值兜底为向量检索，避免 NPE 导致整次检索失败。
     */
    private SearchMode resolveMode(String searchMode) {
        String mode = StringUtils.defaultIfBlank(searchMode, SearchType.EMBEDDING);
        return switch (mode) {
            case SearchType.FULL_TEXT -> SearchMode.FULL_TEXT;
            case SearchType.HYBRID -> SearchMode.HYBRID;
            default -> SearchMode.VECTOR;
        };
    }

    private IDataStore getStore(SearchMode mode) {
        return switch (mode) {
            case SearchMode.FULL_TEXT -> fullTextStore;
            case SearchMode.HYBRID -> compositeStore;
            default -> vectorStore;
        };
    }
}