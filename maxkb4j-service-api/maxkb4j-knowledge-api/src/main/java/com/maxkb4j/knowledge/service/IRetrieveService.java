package com.maxkb4j.knowledge.service;

import com.maxkb4j.common.domain.base.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.vo.ParagraphVO;

import java.util.List;

public interface IRetrieveService {
    List<ParagraphVO> paragraphSearch(String question, List<String> knowledgeIds, List<String> excludeParagraphIds, KnowledgeSetting datasetSetting);
}
