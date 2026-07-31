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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检索编排器：承担原先泄漏进各 store 的检索策略——
 * 排除非激活段落、段落路与问题路双路召回、问题到段落的映射、按 paragraphId 去重排序截断。
 *
 * <p>store 层因此退化为纯持久化端口（{@link IDataStore#searchBySource}），不再依赖任何 service，
 * 原构造期循环依赖（store 与 service 互相注入、用 {@code ObjectProvider} 兜底）随之消除。</p>
 *
 * <p>编排顺序：先解析排除段落 ID 填入 {@link SearchRequest}，再分别按段落、问题来源取原始命中，
 * 把问题命中经 problem_paragraph 映射为段落，最后合并去重并截断到 topK。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchOrchestrator {

    private final IParagraphInternalService paragraphService;
    private final IProblemParagraphService problemParagraphService;

    /**
     * 在指定 store 上执行双路召回编排。
     * @param store 由调用方按检索模式选定的后端（vector / fullText / composite）
     */
    public List<TextChunkVO> search(IDataStore store, SearchRequest request) {
        if (shouldShortCircuit(request)) {
            return Collections.emptyList();
        }
        resolveExcludeParagraphIds(request);

        List<TextChunkVO> results = new ArrayList<>(store.searchBySource(request, SourceType.PARAGRAPH));
        List<TextChunkVO> problemHits = store.searchBySource(request, SourceType.PROBLEM);
        results.addAll(mapProblemsToParagraphs(problemHits));
        return dedupAndRank(results, request.getTopK());
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
     * 对原始检索结果按 paragraphId 做去重 + 排序 + 截断：
     *   1) 先按 paragraphId 累加 totalScore（用于同分时的 tiebreaker）
     *   2) 按 score 降序排序
     *   3) 每个 paragraphId 仅保留 score 最高的一条
     *   4) 同 score 的条目按 paragraphId 累计总分降序
     *   5) 截断到 topK
     */
    private List<TextChunkVO> dedupAndRank(List<TextChunkVO> raw, int topK) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> totalScoreByParagraphId = new HashMap<>();
        for (TextChunkVO result : raw) {
            totalScoreByParagraphId.merge(result.getSourceId(), result.getScore(), Double::sum);
        }

        List<TextChunkVO> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingDouble(TextChunkVO::getScore).reversed());

        List<TextChunkVO> distinct = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TextChunkVO item : sorted) {
            if (seen.add(item.getSourceId())) {
                distinct.add(item);
            }
        }

        distinct.sort((a, b) -> {
            int scoreCompare = Double.compare(b.getScore(), a.getScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            double totalA = totalScoreByParagraphId.getOrDefault(a.getSourceId(), 0.0);
            double totalB = totalScoreByParagraphId.getOrDefault(b.getSourceId(), 0.0);
            return Double.compare(totalB, totalA);
        });

        int end = Math.min(topK, distinct.size());
        return distinct.subList(0, end);
    }
}
