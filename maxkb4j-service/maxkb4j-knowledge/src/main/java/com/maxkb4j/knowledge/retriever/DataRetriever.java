package com.maxkb4j.knowledge.retriever;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.consts.SearchType;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.retrieval.IDataRetriever;
import com.maxkb4j.knowledge.retrieval.SearchMode;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IDocumentService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Unified data retriever that supports multiple search modes
 */
@Slf4j
@Component
public class DataRetriever implements IDataRetriever {

    private final IDataStore vectorStore;
    private final IDataStore fullTextStore;
    private final IDataStore compositeStore;
    private final IDocumentService documentService;

    public DataRetriever(@Qualifier("vectorStore") IDataStore vectorStore,
                         @Qualifier("fullTextStore") IDataStore fullTextStore,
                         @Qualifier("compositeStore") IDataStore compositeStore,
                         IDocumentService documentService) {
        this.vectorStore = vectorStore;
        this.fullTextStore = fullTextStore;
        this.compositeStore = compositeStore;
        this.documentService = documentService;
    }

    private static final Map<String, SearchMode> SEARCH_MODE_MAP = Map.of(
        SearchType.EMBEDDING, SearchMode.VECTOR,
        SearchType.FULL_TEXT, SearchMode.FULL_TEXT,
        SearchType.HYBRID, SearchMode.HYBRID
    );

    @Override
    public List<TextChunkVO> search(List<String> knowledgeIds, List<String> excludeParagraphIds,
                                     String keyword, int maxResults, float minScore, String searchMode) {
        return search(knowledgeIds, excludeParagraphIds, keyword, maxResults, minScore, searchMode, null);
    }

    @Override
    public List<TextChunkVO> search(List<String> knowledgeIds, List<String> excludeParagraphIds,
                                     String keyword, int maxResults, float minScore, String searchMode, String chatModelId) {
        SearchRequest request = new SearchRequest();
        request.setKnowledgeIds(knowledgeIds);
        request.setExcludeParagraphIds(excludeParagraphIds);
        request.setQuery(keyword);
        request.setTopK(maxResults);
        request.setMinScore(minScore);
        request.setMode(SEARCH_MODE_MAP.get(searchMode));
        List<DocumentEntity> excludeDocuments = documentService.lambdaQuery()
                .select(DocumentEntity::getId)
                .in(DocumentEntity::getKnowledgeId, knowledgeIds)
                .eq(DocumentEntity::getIsActive, false)
                .list();
        if (CollectionUtils.isNotEmpty(excludeDocuments)){
            request.setExcludeDocumentIds(excludeDocuments.stream().map(DocumentEntity::getId).toList());
        }
        return getStore(searchMode).search(request);
    }

    private IDataStore getStore(String searchMode) {
        return switch (searchMode) {
            case SearchType.EMBEDDING -> vectorStore;
            case SearchType.FULL_TEXT -> fullTextStore;
            case SearchType.HYBRID -> compositeStore;
            default -> throw new IllegalArgumentException("Unknown search mode: " + searchMode);
        };
    }
}
