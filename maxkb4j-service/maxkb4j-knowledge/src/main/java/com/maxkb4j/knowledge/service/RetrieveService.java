package com.maxkb4j.knowledge.service;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.dto.DataSearchDTO;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.retrieval.IDataRetriever;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RetrieveService implements IRetrieveService{

    private final ParagraphMapper paragraphMapper;
    private final IDataRetriever dataRetriever;


    private List<TextChunkVO> dataSearch(List<String> knowledgeIds, DataSearchDTO dto) {
        if (CollectionUtils.isEmpty(knowledgeIds)) {
            return Collections.emptyList();
        }
        return dataRetriever.search(knowledgeIds, dto.getExcludeParagraphIds(),
                                dto.getQueryText(), dto.getTopNumber(),
                                dto.getSimilarity(), dto.getSearchMode());
    }

    public List<ParagraphVO> paragraphSearch(String question, List<String> knowledgeIds, List<String> excludeParagraphIds, KnowledgeSetting datasetSetting) {
        DataSearchDTO dto = new DataSearchDTO();
        dto.setQueryText(question);
        dto.setSearchMode(datasetSetting.getSearchMode());
        dto.setSimilarity(datasetSetting.getSimilarity());
        dto.setTopNumber(datasetSetting.getTopN());
        dto.setExcludeParagraphIds(excludeParagraphIds);
        return paragraphSearch(knowledgeIds, dto);
    }

    public List<ParagraphVO> paragraphSearch(List<String> knowledgeIds, DataSearchDTO dto) {
        List<TextChunkVO> list = dataSearch(knowledgeIds, dto);
        List<String> paragraphIds = list.stream().map(TextChunkVO::getParagraphId).toList();
        if (CollectionUtils.isEmpty(paragraphIds)) {
            return Collections.emptyList();
        }
        Map<String, Float> scoreMap = list.stream().collect(Collectors.toMap(TextChunkVO::getParagraphId, TextChunkVO::getScore));
        // 记录 paragraphIds 的顺序索引
        Map<String, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < paragraphIds.size(); i++) {
            orderMap.put(paragraphIds.get(i), i);
        }
        List<ParagraphVO> paragraphs = paragraphMapper.retrievalParagraph(paragraphIds);
        paragraphs.forEach(e -> {
            float score = scoreMap.get(e.getId());
            e.setSimilarity(score);
            e.setComprehensiveScore(score);
            if (e.getDocumentName()==null){
                e.setDocumentName("");
            }
        });
        // 按照 paragraphIds 的原始顺序排序
        paragraphs.sort(Comparator.comparingInt(p -> orderMap.get(p.getId())));
        return paragraphs;
    }
}
