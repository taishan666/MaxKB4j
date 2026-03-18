package com.maxkb4j.knowledge.retrieval;


import com.maxkb4j.knowledge.vo.TextChunkVO;

import java.util.List;

public interface IDataRetriever {

    List<TextChunkVO> search(List<String> knowledgeIds, List<String> excludeParagraphIds,
                             String keyword, int maxResults, float minScore, String searchMode);
}
