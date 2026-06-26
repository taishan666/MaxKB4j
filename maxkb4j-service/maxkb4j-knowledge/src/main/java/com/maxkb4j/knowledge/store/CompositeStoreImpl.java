package com.maxkb4j.knowledge.store;

import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 组合 store：同时写入向量与全文两路，搜索时混合两路结果。
 * <p>写操作通过 {@link #dualWrite} 串行委托并对失败做统一日志/异常包装；
 * 搜索通过 {@link #safeSearchAsync} 并发执行并支持单路降级（一路故障仍返回另一路结果）。</p>
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
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        dualWrite("deleteByProblemIdAndParagraphId",
                s -> s.deleteByProblemIdAndParagraphId(knowledgeId, problemId, paragraphId));
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        dualWrite("deleteProblemByIds", s -> s.deleteProblemByIds(knowledgeId, problemIds));
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

    /**
     * 同时检索向量库与全文库，按 paragraphId 取较高分融合后排序截断到 topK。
     * 任意一路检索异常会被降级为空列表，另一路结果仍然返回。
     */
    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        CompletableFuture<List<TextChunkVO>> vectorFuture = safeSearchAsync(vectorStore, request, "vector");
        CompletableFuture<List<TextChunkVO>> fullTextFuture = safeSearchAsync(fullTextStore, request, "fullText");
        List<TextChunkVO> vectorHits = vectorFuture.join();
        List<TextChunkVO> fullTextHits = fullTextFuture.join();
        return mergeByMaxScore(vectorHits, fullTextHits, request.getTopK());
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
    private CompletableFuture<List<TextChunkVO>> safeSearchAsync(IDataStore store, SearchRequest request, String tag) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return store.search(request);
            } catch (Exception e) {
                log.error("Composite sub-search [{}] failed, fallback to empty: {}", tag, e.getMessage(), e);
                return Collections.<TextChunkVO>emptyList();
            }
        });
    }

    /**
     * 按 paragraphId 取两路结果中的较高 score，按 score 降序后截断到 topK。
     */
    private List<TextChunkVO> mergeByMaxScore(List<TextChunkVO> vectorHits, List<TextChunkVO> fullTextHits, int topK) {
        Map<String, Double> maxScoreByParagraphId = new LinkedHashMap<>();
        for (List<TextChunkVO> hits : List.of(vectorHits, fullTextHits)) {
            for (TextChunkVO hit : hits) {
                maxScoreByParagraphId.merge(hit.getParagraphId(), hit.getScore(), Math::max);
            }
        }
        List<TextChunkVO> merged = new ArrayList<>(maxScoreByParagraphId.size());
        maxScoreByParagraphId.forEach((paragraphId, score) -> merged.add(new TextChunkVO(paragraphId, score)));
        merged.sort(Comparator.comparing(TextChunkVO::getScore).reversed());
        int end = Math.min(topK, merged.size());
        return merged.subList(0, end);
    }
}
