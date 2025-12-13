package com.tarzan.maxkb4j.core.chatpipeline.step.searchdatasetstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.chatpipeline.step.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@AllArgsConstructor
public class SearchDatasetStep extends ISearchDatasetStep {

    private final RetrieveService retrieveService;

    @Override
    protected List<ParagraphVO> execute(List<String> knowledgeIdList,KnowledgeSetting datasetSetting, String problemText, String paddingProblemText, Boolean reChat, PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        List<String> excludeParagraphIds = reChat ? manage.getExcludeParagraphIds(problemText) : List.of();
        List<CompletableFuture<List<ParagraphVO>>> futureList = new ArrayList<>();
        futureList.add(CompletableFuture.supplyAsync(() -> retrieveService.paragraphSearch(problemText, knowledgeIdList, excludeParagraphIds, datasetSetting)));
        if (StringUtils.isNotBlank(paddingProblemText) && !problemText.equals(paddingProblemText)) {
            futureList.add(CompletableFuture.supplyAsync(() -> retrieveService.paragraphSearch(paddingProblemText, knowledgeIdList, excludeParagraphIds, datasetSetting)));
        }
        List<ParagraphVO> paragraphList = futureList.stream().flatMap(future -> future.join().stream()).toList();
        if (paragraphList.size() > datasetSetting.getTopN()) {
            Map<String, ParagraphVO> map = new LinkedHashMap<>();
            //融合排序
            for (ParagraphVO paragraph : paragraphList) {
                if (map.containsKey(paragraph.getId())) {
                    if (map.get(paragraph.getId()).getComprehensiveScore() < paragraph.getComprehensiveScore()) {
                        map.put(paragraph.getId(), paragraph);
                    }
                } else {
                    map.put(paragraph.getId(), paragraph);
                }
            }
            List<ParagraphVO> results = new ArrayList<>(map.values());
            results.sort(Comparator.comparing(ParagraphVO::getComprehensiveScore).reversed());
            int endIndex = Math.min(datasetSetting.getTopN(), results.size());
            paragraphList = results.subList(0, endIndex);
        }
        log.info("dataset search 耗时 {} ms", System.currentTimeMillis() - startTime);
        context.put("paragraphList", paragraphList);
        context.put("problemText", problemText);
        return paragraphList;
    }


    @Override
    public JSONObject getDetails() {
        JSONObject details = new JSONObject();
        details.put("step_type", "search_step");
        details.put("paragraphList", context.get("paragraphList"));
        details.put("runTime", context.get("runTime"));
        details.put("problemText", context.get("problemText"));
        details.put("messageTokens", context.getOrDefault("messageTokens", 0));
        details.put("answerTokens", context.getOrDefault("answerTokens", 0));
        return details;
    }
}
