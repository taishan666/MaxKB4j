package com.tarzan.maxkb4j.module.dataset.service;

import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RetrieveService {

    private final EmbedTextService embedTextService;
    private final ParagraphMapper paragraphMapper;
    private final FullTextIndexService fullTextIndexService;


    public List<ParagraphVO> paragraphSearch(String question, List<String> datasetIds, List<String> excludeParagraphIds, int TopN, float similarity, String searchMode) {
        HitTestDTO dto = new HitTestDTO();
        dto.setQueryText(question);
        dto.setSearchMode(searchMode);
        dto.setSimilarity(similarity);
        dto.setTopNumber(TopN);
        return paragraphSearch(datasetIds, dto);
    }

    private List<HitTestVO> dataSearch(List<String> datasetIds, HitTestDTO dto) {
        if ("embedding".equals(dto.getSearchMode())) {
            return embedTextService.search(datasetIds, dto.getQueryText(), dto.getTopNumber(), dto.getSimilarity());
        }
        if ("keywords".equals(dto.getSearchMode())) {
            return fullTextIndexService.search(datasetIds, dto.getQueryText(), dto.getTopNumber());
        }
        if ("blend".equals(dto.getSearchMode())) {
            Map<String, Float> map = new LinkedHashMap<>();
            List<HitTestVO> results = new ArrayList<>();
            List<HitTestVO> embedResults = embedTextService.search(datasetIds, dto.getQueryText(), dto.getTopNumber(), dto.getSimilarity());
            List<HitTestVO> fullTextResults = fullTextIndexService.search(datasetIds, dto.getQueryText(), dto.getTopNumber());
            //融合排序
            for (HitTestVO embedResult : embedResults) {
                map.put(embedResult.getParagraphId(), embedResult.getScore());
            }
            for (HitTestVO fullTextResult : fullTextResults) {
                if (map.containsKey(fullTextResult.getParagraphId())) {
                    if (map.get(fullTextResult.getParagraphId()) < fullTextResult.getScore()) {
                        map.put(fullTextResult.getParagraphId(), fullTextResult.getScore());
                    }
                } else {
                    map.put(fullTextResult.getParagraphId(), fullTextResult.getScore());
                }
            }
            map.forEach((key, value) -> results.add(new HitTestVO(key, value)));
            int endIndex = Math.min(dto.getTopNumber(), results.size());
            return results.subList(0, endIndex);
        }
        return Collections.emptyList();
    }

    public List<ParagraphVO> paragraphSearch(List<String> datasetIds, HitTestDTO dto) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return Collections.emptyList();
        }
        List<HitTestVO> list = dataSearch(datasetIds, dto);
        List<String> paragraphIds = list.stream().map(HitTestVO::getParagraphId).toList();
        if (CollectionUtils.isEmpty(paragraphIds)) {
            return Collections.emptyList();
        }
        Map<String, Float> map = list.stream().collect(Collectors.toMap(HitTestVO::getParagraphId, HitTestVO::getScore));
        List<ParagraphVO> paragraphs = paragraphMapper.retrievalParagraph(paragraphIds);
        paragraphs.forEach(e -> {
            float score = map.get(e.getId());
            e.setSimilarity(score);
            e.setComprehensiveScore(score);
        });
        paragraphs.sort(Comparator.comparing(ParagraphVO::getSimilarity).reversed());
        return paragraphs;
    }
}
