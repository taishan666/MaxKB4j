package com.maxkb4j.knowledge.store;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检索/写入 store 的抽象基类，承担参数校验、结果去重排序等模板职责
 */
public abstract class BaseStoreImpl implements IDataStore {

    /**
     * 汇总搜索时需要排除的 paragraphId 集合：
     *   1) 通过 {@link IParagraphService#noActiveList} 查询出非活跃段落（按 knowledgeIds + excludeDocumentIds 过滤）
     *   2) 合并调用方在 {@link SearchRequest#getExcludeParagraphIds()} 显式传入的 ID
     * <p>主要给不支持就地更新 metadata 的向量后端（如 langchain4j 的 PgVectorEmbeddingStore）使用，
     * 在检索时通过 filter 过滤掉 isActive=false 的段落。</p>
     */
    protected List<String> resolveExcludeParagraphIds(SearchRequest request, IParagraphService paragraphService) {
        List<String> excludeParagraphIds = new ArrayList<>();
        List<String> noActiveParagraphIds = paragraphService.noActiveList(
                request.getKnowledgeIds(), request.getExcludeDocumentIds());
        if (CollectionUtils.isNotEmpty(noActiveParagraphIds)) {
            excludeParagraphIds.addAll(noActiveParagraphIds);
        }
        if (CollectionUtils.isNotEmpty(request.getExcludeParagraphIds())) {
            excludeParagraphIds.addAll(request.getExcludeParagraphIds());
        }
        return excludeParagraphIds;
    }

    /**
     * 搜索前置校验：knowledgeIds 或 query 为空时短路
     */
    protected boolean shouldShortCircuit(SearchRequest request) {
        if (request == null) {
            return true;
        }
        if (request.getKnowledgeIds() == null || request.getKnowledgeIds().isEmpty()) {
            return true;
        }
        return StringUtils.isBlank(request.getQuery());
    }

    /**
     * 对原始检索结果按 paragraphId 做去重 + 排序 + 截断：
     *   1) 先按 paragraphId 累加 totalScore（用于同分时的 tiebreaker）
     *   2) 按 score 降序排序
     *   3) 每个 paragraphId 仅保留 score 最高的一条
     *   4) 同 score 的条目按 paragraphId 累计总分降序
     *   5) 截断到 topK
     * 抽取自 VectorStoreImpl / PgVectorEmbeddingStoreImpl 中完全重复的逻辑段
     */
    protected List<TextChunkVO> dedupAndRank(List<TextChunkVO> raw, int topK) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> totalScoreByParagraphId = new HashMap<>();
        for (TextChunkVO result : raw) {
            totalScoreByParagraphId.merge(result.getParagraphId(), result.getScore(), Double::sum);
        }

        List<TextChunkVO> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingDouble(TextChunkVO::getScore).reversed());

        List<TextChunkVO> distinct = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TextChunkVO item : sorted) {
            if (seen.add(item.getParagraphId())) {
                distinct.add(item);
            }
        }

        distinct.sort((a, b) -> {
            int scoreCompare = Double.compare(b.getScore(), a.getScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            double totalA = totalScoreByParagraphId.getOrDefault(a.getParagraphId(), 0.0);
            double totalB = totalScoreByParagraphId.getOrDefault(b.getParagraphId(), 0.0);
            return Double.compare(totalB, totalA);
        });

        int end = Math.min(topK, distinct.size());
        return distinct.subList(0, end);
    }
}
