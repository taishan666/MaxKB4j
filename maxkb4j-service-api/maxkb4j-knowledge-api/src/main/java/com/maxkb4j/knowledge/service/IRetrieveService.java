package com.maxkb4j.knowledge.service;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;

import java.util.List;

public interface IRetrieveService {

    List<ParagraphRagVO> paragraphSearch(String question, List<String> knowledgeIds, List<String> excludeParagraphIds, KnowledgeSetting datasetSetting);

    /**
     * 指定召回条数的检索，供 rerank 场景超采样使用。
     *
     * @param topNumber 召回条数上限；null 时回退 {@code datasetSetting.topN}
     */
    default List<ParagraphRagVO> paragraphSearch(String question, List<String> knowledgeIds, List<String> excludeParagraphIds, KnowledgeSetting datasetSetting, Integer topNumber) {
        return paragraphSearch(question, knowledgeIds, excludeParagraphIds, datasetSetting);
    }
}
