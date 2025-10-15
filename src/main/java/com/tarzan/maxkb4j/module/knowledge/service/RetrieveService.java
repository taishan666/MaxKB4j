package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DataSearchDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextChunkVO;
import com.tarzan.maxkb4j.module.knowledge.mapper.ParagraphMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RetrieveService {

    private final ParagraphMapper paragraphMapper;
    private final Map<String, IDataRetriever> dataRetrieverMap;


    public List<ParagraphVO> paragraphSearch(String question, List<String> knowledgeIds, List<String> excludeParagraphIds, KnowledgeSetting datasetSetting) {
        DataSearchDTO dto = new DataSearchDTO();
        dto.setQueryText(question);
        dto.setSearchMode(datasetSetting.getSearchMode());
        dto.setSimilarity(datasetSetting.getSimilarity());
        dto.setTopNumber(datasetSetting.getTopN());
        dto.setExcludeParagraphIds(excludeParagraphIds);
        return paragraphSearch(knowledgeIds, dto);
    }

    private List<TextChunkVO> dataSearch(List<String> knowledgeIds,DataSearchDTO dto) {
        if (CollectionUtils.isEmpty(knowledgeIds)) {
            return Collections.emptyList();
        }
        IDataRetriever dataRetriever=dataRetrieverMap.get(dto.getSearchMode());
        if (dataRetriever == null){
            return Collections.emptyList();
        }
        return dataRetriever.search(knowledgeIds,dto.getExcludeParagraphIds(), dto.getQueryText(), dto.getTopNumber(), dto.getSimilarity());
    }

    public List<ParagraphVO> paragraphSearch(List<String> knowledgeIds, DataSearchDTO dto) {
        List<TextChunkVO> list = dataSearch(knowledgeIds, dto);
        List<String> paragraphIds = list.stream().map(TextChunkVO::getParagraphId).toList();
        if (CollectionUtils.isEmpty(paragraphIds)) {
            return Collections.emptyList();
        }
        Map<String, Float> map = list.stream().collect(Collectors.toMap(TextChunkVO::getParagraphId, TextChunkVO::getScore));
        List<ParagraphVO> paragraphs = paragraphMapper.retrievalParagraph(paragraphIds);
        paragraphs.forEach(e -> {
            float score = map.get(e.getId());
            e.setSimilarity(score);
            e.setComprehensiveScore(score);
            if (e.getDocumentName()==null){
                e.setDocumentName("");
            }
        });
        paragraphs.sort(Comparator.comparing(ParagraphVO::getSimilarity).reversed());
        return paragraphs;
    }
}
