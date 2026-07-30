package com.maxkb4j.knowledge.service;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;

import java.util.List;

public interface IRetrieveService {
    List<ParagraphRagVO> paragraphSearch(String question, List<String> knowledgeIds, List<String> excludeParagraphIds, KnowledgeSetting datasetSetting);
}
