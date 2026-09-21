package com.maxkb4j.application.pipeline.step.searchdatasetstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.pipeline.step.searchdatasetstep.AbsSearchDatasetStep;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.service.IRetrieveService;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchDatasetStep extends AbsSearchDatasetStep {

    /**
     * rerank 开启时的召回超采样倍数：多召回候选交给 RerankStep 精排筛选，
     * 避免粗排名次截断把 rerank 后本应入选的段落挡在门外。
     */
    private static final int RERANK_RECALL_MULTIPLIER = 3;

    private final IRetrieveService retrieveService;
    private final TaskExecutor taskExecutor;

    @Override
    protected List<ParagraphRagVO> execute(List<String> knowledgeIds, KnowledgeSetting datasetSetting, String problemText, String paddingProblemText, Boolean reChat, PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        List<ParagraphRagVO> paragraphList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(knowledgeIds) && !Boolean.TRUE.equals(datasetSetting.getOnDemandEnable())) {
            List<String> excludeParagraphIds = reChat ? manage.getExcludeParagraphIds(problemText) : List.of();
            paragraphList = retrieval(knowledgeIds, datasetSetting, problemText, paddingProblemText, excludeParagraphIds);
        }
        log.info("dataset search 耗时 {} ms", System.currentTimeMillis() - startTime);
        super.context.put("paragraphList", paragraphList);
        super.context.put("problemText", problemText);
        return paragraphList;
    }

    protected List<ParagraphRagVO> retrieval(List<String> knowledgeIds, KnowledgeSetting datasetSetting, String problemText, String paddingProblemText, List<String> excludeParagraphIds) {
        // rerank 开启时召回超采样：多取候选留给 RerankStep 精排截断；未开启时维持 topN
        int recallN = resolveRecallNumber(datasetSetting);
        List<CompletableFuture<List<ParagraphRagVO>>> futureList = new ArrayList<>();
        futureList.add(CompletableFuture.supplyAsync(
                () -> retrieveService.paragraphSearch(problemText, knowledgeIds, excludeParagraphIds, datasetSetting, recallN), taskExecutor));
        // 问题优化（改写）结果与原文并行召回
        if (StringUtils.isNotBlank(paddingProblemText) && !problemText.equals(paddingProblemText)) {
            futureList.add(CompletableFuture.supplyAsync(
                    () -> retrieveService.paragraphSearch(paddingProblemText, knowledgeIds, excludeParagraphIds, datasetSetting, recallN), taskExecutor));
        }
        // 多路查询结果融合：同一 paragraphId 保留最高 similarity，避免重复段落挤占名额
        Comparator<ParagraphRagVO> bySimilarity = Comparator.comparing(ParagraphRagVO::getSimilarity,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        List<ParagraphRagVO> results = new ArrayList<>(futureList.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toMap(
                        ParagraphRagVO::getId,
                        Function.identity(),
                        BinaryOperator.maxBy(bySimilarity),
                        LinkedHashMap::new))
                .values());
        results.sort(bySimilarity.reversed());
        return results.size() <= recallN ? results : results.subList(0, recallN);
    }

    /**
     * 召回条数：rerank 开启（含模型配置）时 topN × {@link #RERANK_RECALL_MULTIPLIER}，否则 topN。
     */
    private static int resolveRecallNumber(KnowledgeSetting setting) {
        int topN = setting.getTopN() == null ? Integer.MAX_VALUE : setting.getTopN();
        boolean rerankEnabled = Boolean.TRUE.equals(setting.getRerankEnable())
                && StringUtils.isNotBlank(setting.getRerankModelId());
        return rerankEnabled && topN != Integer.MAX_VALUE ? topN * RERANK_RECALL_MULTIPLIER : topN;
    }


    @Override
    public JSONObject getDetails() {
        JSONObject details = new JSONObject(true);
        details.put("step_type", "search_step");
        details.put("paragraphList", context.get("paragraphList"));
        details.put("runTime", context.get("runTime"));
        details.put("problemText", context.get("problemText"));
        details.put("messageTokens", context.getOrDefault("messageTokens", 0));
        details.put("answerTokens", context.getOrDefault("answerTokens", 0));
        return details;
    }
}
