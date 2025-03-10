package com.tarzan.maxkb4j.module.dataset.service;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.embedding.mapper.EmbeddingMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RetrieveService {

    private final EmbeddingMapper embeddingMapper;
    private final ParagraphMapper paragraphMapper;
    private final JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();
    private final DatasetBaseService datasetService;
    private final TextSegmentService fullTextSearchService;


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
        EmbeddingModel embeddingModel=datasetService.getDatasetEmbeddingModel(datasetIds.get(0));
        Response<Embedding> res = embeddingModel.embed(dto.getQuery_text());
        if ("embedding".equals(dto.getSearch_mode())) {
            System.out.println("embedding 耗时 "+(System.currentTimeMillis()-startTime)+" ms");
            return embeddingMapper.embeddingSearch(datasetIds, dto.getTop_number(),dto.getSimilarity(), res.content().vector());
        }
        if ("keywords".equals(dto.getSearch_mode())) {
            System.out.println("keywords 耗时 "+(System.currentTimeMillis()-startTime)+" ms");
           // dto.setQuery_text(toTsQuery(dto.getQuery_text()));
            List<EmbeddingEntity> results = fullTextSearchService.search(datasetIds,dto.getQuery_text(), dto.getTop_number());
            for (EmbeddingEntity result : results) {
                System.out.println(result.getParagraphId());
                System.out.println(result.getScore());
            }
            System.out.println("fullTextSearchService 耗时 "+(System.currentTimeMillis()-startTime)+" ms");
            return new ArrayList<>();
           // return embeddingMapper.keywordsSearch(datasetIds, dto);
        }
        if ("blend".equals(dto.getSearch_mode())) {
            return embeddingMapper.HybridSearch(datasetIds, dto, res.content().vector());
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
        System.out.println("paragraphSearch1 耗时 "+(System.currentTimeMillis()-startTime)+" ms");
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
