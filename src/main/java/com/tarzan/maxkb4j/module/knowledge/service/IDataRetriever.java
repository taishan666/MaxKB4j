package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextChunkVO;

import java.util.List;

public interface IDataRetriever {

    List<TextChunkVO> search(List<String> datasetIds, List<String> excludeParagraphIds, String keyword, int maxResults, float minScore);
}
