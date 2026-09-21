package com.maxkb4j.knowledge.store.impl;

import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 组合 store：同时写入向量与全文两路，搜索时按来源类型混合两路结果。
 * <p>写操作通过 {@link #dualWrite} 串行委托并对失败做统一日志/异常包装；
 * 搜索通过 {@link #safeSearchAsync} 并发执行并支持单路降级（一路故障仍返回另一路结果）。</p>
 * <p>段落路与问题路的编排、问题到段落的映射以及去重排序由
 * {@link com.maxkb4j.knowledge.retriever.SearchOrchestrator} 负责，本类只做同源两后端融合。</p>
 */
@Slf4j
@Component("compositeStore")
public class CompositeStoreImpl extends BaseStoreImpl {

    private final IDataStore vectorStore;
    private final IDataStore fullTextStore;

    public CompositeStoreImpl(@Qualifier("vectorStore") IDataStore vectorStore,
                              @Qualifier("fullTextStore") IDataStore fullTextStore) {
        this.vectorStore = vectorStore;
        this.fullTextStore = fullTextStore;
    }

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        log.debug("Upserting {} entities to both PostgreSQL and MongoDB", entities.size());
        dualWrite("upsert", s -> s.upsert(model, entities));
    }


    @Override
    public void deleteByProblemIds(String knowledgeId, List<String> problemIds) {
        dualWrite("deleteByProblemIds", s -> s.deleteByProblemIds(knowledgeId, problemIds));
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        dualWrite("deleteByParagraphIds", s -> s.deleteByParagraphIds(knowledgeId, paragraphIds));
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        dualWrite("deleteByDocumentIds", s -> s.deleteByDocumentIds(knowledgeId, documentIds));
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        dualWrite("deleteByKnowledgeId", s -> s.deleteByKnowledgeId(knowledgeId));
    }

    @Override
    public void deleteByKnowledgeIds(List<String> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return;
        }
        dualWrite("deleteByKnowledgeIds", s -> s.deleteByKnowledgeIds(knowledgeIds));
    }

    /**
     * 同时检索向量库与全文库的同一来源类型，按 RRF（Reciprocal Rank Fusion）融合后排序截断到 topK。
     * 任意一路检索异常会被降级为空列表，另一路结果仍然返回。
     */
    @Override
    public List<TextChunkVO> searchBySource(SearchRequest request, int sourceType) {
        CompletableFuture<List<TextChunkVO>> vectorFuture = safeSearchAsync(vectorStore, request, sourceType, "vector");
        CompletableFuture<List<TextChunkVO>> fullTextFuture = safeSearchAsync(fullTextStore, request, sourceType, "fullText");
        return mergeByRrf(vectorFuture.join(), fullTextFuture.join(), request.getTopK());
    }

    /**
     * 对两路 store 顺序执行同一动作；任一路失败抛 RuntimeException，由调用方感知。
     */
    private void dualWrite(String op, Consumer<IDataStore> action) {
        try {
            action.accept(vectorStore);
            action.accept(fullTextStore);
            log.debug("Composite [{}] succeeded across both stores", op);
        } catch (Exception e) {
            log.error("Composite [{}] failed: {}", op, e.getMessage(), e);
            throw new RuntimeException("Composite store operation [" + op + "] failed", e);
        }
    }

    /**
     * 把单路检索包装成异步任务，异常时返回空列表并打日志，绝不抛给 join()。
     */
    private CompletableFuture<List<TextChunkVO>> safeSearchAsync(IDataStore store, SearchRequest request, int sourceType, String tag) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return store.searchBySource(request, sourceType);
            } catch (Exception e) {
                log.error("Composite sub-search [{}] failed, fallback to empty: {}", tag, e.getMessage(), e);
                return Collections.<TextChunkVO>emptyList();
            }
        });
    }

    /**
     * RRF 平滑常数，取业界惯例 60。
     */
    private static final int RRF_K = 60;

    /**
     * Reciprocal Rank Fusion 融合两路命中：{@code score(d) = Σ 1/(k + rank_route(d))}。
     *
     * <p>RRF 只依赖名次不依赖分值，规避了向量路（归一化余弦）与全文路（归一化 textScore）
     * 分数量纲不一致导致的排序失真——原先按两路取 max 融合，单一后端的分值尺度主导排序，
     * hybrid 效果常不如单路。两路同时命中的段落天然获得名次加成。</p>
     *
     * <p>返回条目的 score 采用两路中的最高归一化分（[0,1] 量纲）：minScore 阈值过滤
     * 已在各子路内完成（同量纲语义正确），下游的命中直答阈值与前端展示分数保持原有语义。</p>
     */
    private List<TextChunkVO> mergeByRrf(List<TextChunkVO> vectorHits, List<TextChunkVO> fullTextHits, int topK) {
        // value: [rrf 累计名次分, 两路最高归一化分]
        Map<String, double[]> fused = new LinkedHashMap<>();
        accumulateRrf(vectorHits, fused);
        accumulateRrf(fullTextHits, fused);
        return fused.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, double[]> e) -> e.getValue()[0]).reversed())
                .limit(Math.max(topK, 0))
                .map(e -> new TextChunkVO(e.getKey(), e.getValue()[1]))
                .toList();
    }

    /**
     * 单路命中按名次累计 RRF 分；路内列表已按分数降序（store 层保证）。
     */
    private static void accumulateRrf(List<TextChunkVO> hits, Map<String, double[]> fused) {
        if (hits == null) {
            return;
        }
        for (int i = 0; i < hits.size(); i++) {
            TextChunkVO hit = hits.get(i);
            double[] acc = fused.computeIfAbsent(hit.getSourceId(), id -> new double[2]);
            acc[0] += 1.0 / (RRF_K + i + 1);
            acc[1] = Math.max(acc[1], hit.getScore() == null ? 0D : hit.getScore());
        }
    }
}