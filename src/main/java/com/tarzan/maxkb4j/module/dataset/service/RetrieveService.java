package com.tarzan.maxkb4j.module.dataset.service;

import com.tarzan.maxkb4j.module.application.domian.entity.DatasetSetting;
import com.tarzan.maxkb4j.module.dataset.domain.dto.DataSearchDTO;
import com.tarzan.maxkb4j.module.dataset.enums.SearchType;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.domain.vo.TextChunkVO;
import com.tarzan.maxkb4j.module.dataset.domain.vo.ParagraphVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RetrieveService {

    private final EmbedTextService embedTextService;
    private final ParagraphMapper paragraphMapper;
    private final FullTextIndexService fullTextIndexService;


    public List<ParagraphVO> paragraphSearch(String question, List<String> datasetIds, List<String> excludeParagraphIds, int TopN, float similarity, String searchMode) {
        DataSearchDTO dto = new DataSearchDTO();
        dto.setQueryText(question);
        dto.setSearchMode(searchMode);
        dto.setSimilarity(similarity);
        dto.setTopNumber(TopN);
        dto.setExcludeParagraphIds(excludeParagraphIds);
        return paragraphSearch(datasetIds, dto);
    }

    public List<ParagraphVO> paragraphSearch(String question, List<String> datasetIds, List<String> excludeParagraphIds, DatasetSetting datasetSetting) {
        DataSearchDTO dto = new DataSearchDTO();
        dto.setQueryText(question);
        dto.setSearchMode(datasetSetting.getSearchMode());
        dto.setSimilarity(datasetSetting.getSimilarity());
        dto.setTopNumber(datasetSetting.getTopN());
        dto.setExcludeParagraphIds(excludeParagraphIds);
        return paragraphSearch(datasetIds, dto);
    }

    private List<TextChunkVO> dataSearch(List<String> datasetIds,DataSearchDTO dto) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return Collections.emptyList();
        }
        if (SearchType.embedding.name().equals(dto.getSearchMode())) {
            return embedTextService.search(datasetIds,dto.getExcludeParagraphIds(), dto.getQueryText(), dto.getTopNumber(), dto.getSimilarity());
        }
        if (SearchType.keywords.name().equals(dto.getSearchMode())) {
            return fullTextIndexService.search(datasetIds,dto.getExcludeParagraphIds(), dto.getQueryText(), dto.getTopNumber(), dto.getSimilarity());
        }
        if (SearchType.blend.name().equals(dto.getSearchMode())) {
            Map<String, Float> map = new LinkedHashMap<>();
            List<TextChunkVO> results = new ArrayList<>();
            List<CompletableFuture<List<TextChunkVO>>> futureList = new ArrayList<>();
            futureList.add(CompletableFuture.supplyAsync(()->embedTextService.search(datasetIds, dto.getExcludeParagraphIds(),dto.getQueryText(), dto.getTopNumber(), dto.getSimilarity())));
            futureList.add(CompletableFuture.supplyAsync(()->fullTextIndexService.search(datasetIds,dto.getExcludeParagraphIds(), dto.getQueryText(), dto.getTopNumber(), dto.getSimilarity())));
            List<TextChunkVO> retrieveResults = futureList.stream().flatMap(future-> future.join().stream()).toList();
            //融合排序
            for (TextChunkVO result : retrieveResults) {
                if (map.containsKey(result.getParagraphId())) {
                    if (map.get(result.getParagraphId()) < result.getScore()) {
                        map.put(result.getParagraphId(), result.getScore());
                    }
                } else {
                    map.put(result.getParagraphId(), result.getScore());
                }
            }
            map.forEach((key, value) -> results.add(new TextChunkVO(key, value)));
            results.sort(Comparator.comparing(TextChunkVO::getScore).reversed());
            int endIndex = Math.min(dto.getTopNumber(), results.size());
            return results.subList(0, endIndex);
        }
        return Collections.emptyList();
    }

    public List<ParagraphVO> paragraphSearch(List<String> datasetIds, DataSearchDTO dto) {
        List<TextChunkVO> list = dataSearch(datasetIds, dto);
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
        });
        paragraphs.sort(Comparator.comparing(ParagraphVO::getSimilarity).reversed());
        return paragraphs;
    }
}
