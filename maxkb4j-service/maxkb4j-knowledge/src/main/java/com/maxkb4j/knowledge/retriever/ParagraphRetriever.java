package com.maxkb4j.knowledge.retriever;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.dto.DataSearchDTO;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.service.IRetrieveService;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParagraphRetriever implements IRetrieveService {

    private final ParagraphMapper paragraphMapper;
    private final DataRetriever dataRetriever;

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
        if (CollectionUtils.isEmpty(knowledgeIds)) {
            return Collections.emptyList();
        }
        List<TextChunkVO> chunks = dataRetriever.search(knowledgeIds, dto.getExcludeParagraphIds(),
                dto.getQueryText(), dto.getTopNumber(),
                dto.getSimilarity(), dto.getSearchMode());
        if (CollectionUtils.isEmpty(chunks)) {
            return Collections.emptyList();
        }
        List<String> paragraphIds = chunks.stream().map(TextChunkVO::getParagraphId).toList();
        Map<String, ParagraphVO> paragraphMap = paragraphMapper.retrievalParagraph(paragraphIds).stream()
                .collect(Collectors.toMap(ParagraphVO::getId, Function.identity()));
        // 按检索结果的原始顺序回填得分并组装结果
        return chunks.stream()
                .map(chunk -> {
                    ParagraphVO paragraph = paragraphMap.get(chunk.getParagraphId());
                    if (paragraph == null) {
                        return null;
                    }
                    paragraph.setSimilarity(chunk.getScore());
                    paragraph.setComprehensiveScore(chunk.getScore());
                    if (paragraph.getDocumentName() == null) {
                        paragraph.setDocumentName("");
                    }
                    return paragraph;
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
