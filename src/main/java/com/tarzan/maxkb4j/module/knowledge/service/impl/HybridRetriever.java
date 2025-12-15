package com.tarzan.maxkb4j.module.knowledge.service.impl;

import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextChunkVO;
import com.tarzan.maxkb4j.module.knowledge.consts.SearchType;
import com.tarzan.maxkb4j.module.knowledge.service.IDataRetriever;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component(SearchType.HYBRID)
@RequiredArgsConstructor
public class HybridRetriever implements IDataRetriever {

    private final EmbedRetriever embedRetriever;
    private final FullTextRetriever fullTextRetriever;

    @Override
    public List<TextChunkVO> search(List<String> knowledgeIds, List<String> excludeParagraphIds, String keyword, int maxResults, float minScore) {
        Map<String, Float> map = new LinkedHashMap<>();
        List<TextChunkVO> results = new ArrayList<>();
        List<CompletableFuture<List<TextChunkVO>>> futureList = new ArrayList<>();
        futureList.add(CompletableFuture.supplyAsync(()->embedRetriever.search(knowledgeIds, excludeParagraphIds,keyword, maxResults, minScore)));
        futureList.add(CompletableFuture.supplyAsync(()->fullTextRetriever.search(knowledgeIds,excludeParagraphIds, keyword, maxResults, minScore)));
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
        int endIndex = Math.min(maxResults, results.size());
        return results.subList(0, endIndex);
    }
}
