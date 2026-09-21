package com.maxkb4j.knowledge.retriever;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索编排器：承担原先泄漏进各 store 的检索策略——
 * 排除非激活段落、段落路与问题路双路召回、问题到段落的映射、按 paragraphId 去重排序截断。
 *
 * <p>store 层因此退化为纯持久化端口（{@link IDataStore#searchBySource}），不再依赖任何 service，
 * 原构造期循环依赖（store 与 service 互相注入、用 {@code ObjectProvider} 兜底）随之消除。</p>
 *
 * <p>编排顺序：先解析排除段落 ID 填入 {@link SearchRequest}，再按召回放大后的 topK
 * 分别取段落、问题两路原始命中，把问题命中经 problem_paragraph 映射为段落，
 * 最后合并去重并截断回原始 topK。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchOrchestrator {

    private final IParagraphInternalService paragraphService;
    private final IProblemParagraphService problemParagraphService;

    /**
     * 召回放大系数：双路召回阶段按 {@code topK * multiplier} 向 store 多取候选，
     * 合并去重后再统一截断回 topK，避免两类过早截断造成的漏召回：
     * 1) store 层 minScore 过滤发生在 topK 截断之后，导致有效结果不足 topK；
     * 2) 段落路与问题路各自截断后才合并，交叉去重吃掉候选。
     */
    @Value("${knowledge.search.recall-multiplier:3}")
    private int recallMultiplier = 3;

    /**
     * 在指定 store 上执行双路召回编排。
     * <p>注：request 由 {@code DataRetriever} 每次调用新建，这里就地放大 topK 是安全的。</p>
     *
     * @param store 由调用方按检索模式选定的后端（vector / fullText / composite）
     */
    public List<TextChunkVO> search(IDataStore store, SearchRequest request) {
        if (shouldShortCircuit(request) || request.getTopK() <= 0) {
            return Collections.emptyList();
        }
        resolveExcludeParagraphIds(request);

        int topK = request.getTopK();
        request.setTopK(topK * Math.max(recallMultiplier, 1));

        List<TextChunkVO> results = new ArrayList<>(store.searchBySource(request, SourceType.PARAGRAPH));
        results.addAll(mapProblemsToParagraphs(store.searchBySource(request, SourceType.PROBLEM)));
        return dedupAndRank(results, topK);
    }

    private boolean shouldShortCircuit(SearchRequest request) {
        if (request == null) {
            return true;
        }
        if (request.getKnowledgeIds() == null || request.getKnowledgeIds().isEmpty()) {
            return true;
        }
        return StringUtils.isBlank(request.getQuery());
    }

    /**
     * 汇总搜索时需要排除的 paragraphId 集合：非激活段落 + 调用方显式传入的 ID，
     * 回填到 {@link SearchRequest#setExcludeParagraphIds} 供 store 构造过滤条件。
     */
    private void resolveExcludeParagraphIds(SearchRequest request) {
        List<String> excludeParagraphIds = new ArrayList<>();
        List<String> noActiveParagraphIds = paragraphService.getNoActiveParagraphIds(
                request.getKnowledgeIds(), request.getExcludeDocumentIds());
        if (CollectionUtils.isNotEmpty(noActiveParagraphIds)) {
            excludeParagraphIds.addAll(noActiveParagraphIds);
        }
        if (CollectionUtils.isNotEmpty(request.getExcludeParagraphIds())) {
            excludeParagraphIds.addAll(request.getExcludeParagraphIds());
        }
        request.setExcludeParagraphIds(excludeParagraphIds);
    }

    /**
     * 问题路召回后置处理：把命中的 problemId 经 problem_paragraph 映射表转换为关联段落，
     * 分值沿用命中问题的相似度（同一 problemId 多条命中取最高分）。
     */
    private List<TextChunkVO> mapProblemsToParagraphs(List<TextChunkVO> problemHits) {
        if (CollectionUtils.isEmpty(problemHits)) {
            return Collections.emptyList();
        }
        Map<String, Double> problemScoreById = new HashMap<>();
        for (TextChunkVO hit : problemHits) {
            problemScoreById.merge(hit.getSourceId(), hit.getScore(), Math::max);
        }
        List<ProblemParagraphEntity> problemParagraphs = problemParagraphService.getActivePPbyProblemIds(new ArrayList<>(problemScoreById.keySet()));
        List<TextChunkVO> results = new ArrayList<>(problemParagraphs.size());
        for (ProblemParagraphEntity pp : problemParagraphs) {
            Double score = problemScoreById.get(pp.getProblemId());
            if (score != null) {
                results.add(new TextChunkVO(pp.getParagraphId(), score));
            }
        }
        return results;
    }

    /**
     * 对原始检索结果按 paragraphId 去重 + 排序 + 截断到 topK：
     * 1) 按 sourceId 聚合出单条最高分 score 与累计总分 totalScore；
     * 2) 按 score 降序排序，同分时按 totalScore 降序（多路/多次命中的段落优先）；
     * 3) 截断到 topK。
     */
    private List<TextChunkVO> dedupAndRank(List<TextChunkVO> raw, int topK) {
        if (CollectionUtils.isEmpty(raw) || topK <= 0) {
            return Collections.emptyList();
        }
        // value: [该 sourceId 的最高分, 该 sourceId 的累计总分]
        Map<String, double[]> scoreById = new HashMap<>();
        for (TextChunkVO chunk : raw) {
            double score = chunk.getScore() == null ? 0D : chunk.getScore();
            scoreById.compute(chunk.getSourceId(), (id, acc) -> {
                if (acc == null) {
                    return new double[]{score, score};
                }
                acc[1] += score;
                acc[0] = Math.max(acc[0], score);
                return acc;
            });
        }
        return scoreById.entrySet().stream()
                .sorted((a, b) -> {
                    int byBest = Double.compare(b.getValue()[0], a.getValue()[0]);
                    return byBest != 0 ? byBest : Double.compare(b.getValue()[1], a.getValue()[1]);
                })
                .limit(topK)
                .map(e -> new TextChunkVO(e.getKey(), e.getValue()[0]))
                .toList();
    }
}
