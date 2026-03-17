package com.maxkb4j.knowledge.retriever;

import com.maxkb4j.knowledge.consts.SearchType;
import com.maxkb4j.knowledge.retrieval.SearchMode;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.retrieval.IDataRetriever;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Vector embedding retriever using similarity search
 */
@Slf4j
@Component(SearchType.EMBEDDING)
@RequiredArgsConstructor
public class EmbedRetriever  implements IDataRetriever {

    private final IDataStore vectorStore;

    @Override
    public List<TextChunkVO> search(List<String> knowledgeIds, List<String> excludeParagraphIds, String keyword, int maxResults, float minScore) {
        SearchRequest request = new SearchRequest();
        request.setKnowledgeIds(knowledgeIds);
        request.setExcludeParagraphIds(excludeParagraphIds);
        request.setQuery(keyword);
        request.setTopK(maxResults);
        request.setMinScore(minScore);
        request.setMode(SearchMode.VECTOR);
        return vectorStore.search(request);
    }
}