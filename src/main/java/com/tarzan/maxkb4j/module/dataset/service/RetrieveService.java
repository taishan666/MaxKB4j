package com.tarzan.maxkb4j.module.dataset.service;

import com.huaban.analysis.jieba.JiebaSegmenter;
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
    private final JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();
    private final DatasetBaseService datasetService;
    private final FullTextSearchService fullTextSearchService;


    public List<ParagraphVO> paragraphSearch(String question,List<String> datasetIds,List<String> excludeParagraphIds,int TopN,float similarity,String searchMode) {
        HitTestDTO dto=new HitTestDTO();
        dto.setQuery_text(question);
        dto.setSearch_mode(searchMode);
        dto.setSimilarity(similarity);
        dto.setTop_number(TopN);
        return paragraphSearch(datasetIds,dto);
    }

    private List<HitTestVO> dataSearch(List<String> datasetIds, HitTestDTO dto) {
        long startTime = System.currentTimeMillis();
        if ("embedding".equals(dto.getSearch_mode())) {
            return embedTextService.search(datasetIds, dto.getQuery_text(), dto.getTop_number(),dto.getSimilarity());
        }
        if ("keywords".equals(dto.getSearch_mode())) {
            List<HitTestVO> results = fullTextSearchService.search(datasetIds,dto.getQuery_text(), dto.getTop_number());
            System.out.println("fullTextSearchService 耗时 "+(System.currentTimeMillis()-startTime)+" ms");
            return results;
        }
        if ("blend".equals(dto.getSearch_mode())) {
            Map<String,Float> map=new LinkedHashMap<>();
            List<HitTestVO> results =new ArrayList<>();
            List<HitTestVO> embedResults = embedTextService.search(datasetIds, dto.getQuery_text(), dto.getTop_number(),dto.getSimilarity());
            List<HitTestVO> fullTextResults = fullTextSearchService.search(datasetIds,dto.getQuery_text(), dto.getTop_number());
            for (HitTestVO embedResult : embedResults) {
                map.put(embedResult.getParagraphId(), embedResult.getScore());
            }
            for (HitTestVO fullTextResult : fullTextResults) {
                if(map.containsKey(fullTextResult.getParagraphId())){
                    if(map.get(fullTextResult.getParagraphId())<fullTextResult.getScore()){
                        map.put(fullTextResult.getParagraphId(), fullTextResult.getScore());
                    }
                }else {
                    map.put(fullTextResult.getParagraphId(), fullTextResult.getScore());
                }
            }
            map.forEach((key, value) -> {
                results.add(new HitTestVO(key,value));
            });
            int endIndex=Math.min(dto.getTop_number(),results.size());
            return results.subList(0, endIndex);
        }
        return Collections.emptyList();
    }

    public List<ParagraphVO> paragraphSearch(List<String> datasetIds, HitTestDTO dto) {
        long startTime = System.currentTimeMillis();
        if (CollectionUtils.isEmpty(datasetIds)) {
            return Collections.emptyList();
        }
        List<HitTestVO> list = dataSearch(datasetIds, dto);
        System.out.println("dataSearch 耗时 "+(System.currentTimeMillis()-startTime)+" ms");
        List<String> paragraphIds = list.stream().map(HitTestVO::getParagraphId).toList();
        if (CollectionUtils.isEmpty(paragraphIds)) {
            return Collections.emptyList();
        }
        Map<String, Float> map = list.stream().collect(Collectors.toMap(HitTestVO::getParagraphId, HitTestVO::getScore));
        List<ParagraphVO> paragraphs = paragraphMapper.retrievalParagraph(paragraphIds);
        paragraphs.forEach(e -> {
            double score = map.get(e.getId());
            e.setSimilarity(score);
            e.setComprehensiveScore(score);
        });
        return paragraphs;
    }



    private String toTsQuery(String text) {
        List<String> words = filterPunctuation(jiebaSegmenter.sentenceProcess(text));
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word).append("|");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static List<String> filterPunctuation(List<String> words) {
        String[] filteredWords = {"", ",", ".", "。", "=", "，", "、", "：", "；", "（", "）"};
        List<String> result = new ArrayList<>();
        for (String word : words) {
            word = word.toLowerCase().trim();
            for (String filteredWord : filteredWords) {
                if (word.contains(filteredWord)) {
                    word = word.replaceAll(filteredWord, "");
                }
            }
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        return result;
    }
}
