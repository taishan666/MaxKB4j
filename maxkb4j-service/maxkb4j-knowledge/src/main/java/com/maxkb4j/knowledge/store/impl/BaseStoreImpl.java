package com.maxkb4j.knowledge.store.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.impl.ParagraphServiceImpl;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 检索/写入 store 的抽象基类，承担参数校验、结果去重排序等模板职责
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseStoreImpl implements IDataStore {

    /** 召回放大倍数：langchain4j 检索 topK*RECALL_MULTIPLIER 条后在内存里做 paragraphId 去重 */
    private static final int RECALL_MULTIPLIER = 10;


    /**
     * 汇总搜索时需要排除的 paragraphId 集合：
     *   2) 合并调用方在 {@link SearchRequest#getExcludeParagraphIds()} 显式传入的 ID
     * <p>主要给不支持就地更新 metadata 的向量后端（如 langchain4j 的 PgVectorEmbeddingStore）使用，
     * 在检索时通过 filter 过滤掉 isActive=false 的段落。</p>
     */
    protected void resolveExcludeParagraphIds(SearchRequest request, ParagraphServiceImpl paragraphService) {
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

    protected EmbeddingSearchRequest buildParagraphSearchRequest(SearchRequest request,Embedding queryEmbedding,List<String> excludeParagraphIds) {
        Filter filter = metadataKey("knowledgeId").isIn(request.getKnowledgeIds());
        filter = filter.and(metadataKey("sourceType").isEqualTo(String.valueOf(SourceType.PARAGRAPH)));
        if (CollectionUtils.isNotEmpty(request.getExcludeDocumentIds())) {
            filter = filter.and(metadataKey("documentId").isNotIn(request.getExcludeDocumentIds()));
        }
        if (CollectionUtils.isNotEmpty(excludeParagraphIds)) {
            filter = filter.and(metadataKey("sourceId").isNotIn(excludeParagraphIds));
        }
        // 归一化，langchain4j的搜索结果是归一化的
        double normalizedMinScore = (request.getMinScore() + 1.0) / 2.0;
        return EmbeddingSearchRequest.builder()
                .filter(filter)
                .query(request.getQuery().trim())
                .queryEmbedding(queryEmbedding)
                .maxResults(request.getTopK())
                .minScore(normalizedMinScore)
                .build();
    }

    protected EmbeddingSearchRequest buildProblemSearchRequest(SearchRequest request,Embedding queryEmbedding) {
        Filter filter = metadataKey("knowledgeId").isIn(request.getKnowledgeIds());
        filter = filter.and(metadataKey("sourceType").isEqualTo(String.valueOf(SourceType.PROBLEM)));
        // 归一化，langchain4j的搜索结果是归一化的
        double normalizedMinScore = (request.getMinScore() + 1.0) / 2.0;
        return EmbeddingSearchRequest.builder()
                .filter(filter)
                .query(request.getQuery().trim())
                .queryEmbedding(queryEmbedding)
                .maxResults(request.getTopK())
                .minScore(normalizedMinScore)
                .build();
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
