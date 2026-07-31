package com.maxkb4j.knowledge.store.impl;

import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.store.document.EmbeddingDocument;
import com.maxkb4j.knowledge.util.Tokenizer;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * MongoDB implementation of VectorStore for full-text search
 */
@Slf4j
@Component("fullTextStore")
@RequiredArgsConstructor
public class FullTextStoreImpl extends BaseStoreImpl {

    /**
     * MongoDB textScore 的经验归一化上限。
     * <p>MongoDB textScore 由 BM25 加权累加得到，没有理论上界；实际观测下绝大部分命中
     * 都落在 [0, 2] 区间。这里取 max(observedMax, 2.0) 兜底，保证 score 落在 [0, 1]
     * 区间，避免极端值（如单段命中数百次关键词）把整体分布拉爆。</p>
     */
    private static final double SCORE_NORMALIZE_CEILING = 2.0;

    private final MongoTemplate mongoTemplate;

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        List<EmbeddingDocument> documents = entities.stream().map(entity ->
                EmbeddingDocument.builder()
                        .sourceId(entity.getSourceId())
                        .sourceType(entity.getSourceType())
                        .knowledgeId(entity.getKnowledgeId())
                        .documentId(entity.getDocumentId())
                        .content(Tokenizer.segment(entity.getContent()))
                        .build()
        ).toList();
        mongoTemplate.insertAll(documents);
        log.debug("Inserted {} embedding documents into MongoDB", documents.size());
    }


    @Override
    public void deleteByProblemIds(String knowledgeId, List<String> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId)
                .and("sourceType").is(SourceType.PROBLEM)
                .and("sourceId").in(problemIds));
        mongoTemplate.remove(query, EmbeddingDocument.class);
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (paragraphIds == null || paragraphIds.isEmpty()) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId)
                .and("sourceType").is(SourceType.PARAGRAPH)
                .and("sourceId").in(paragraphIds));
        mongoTemplate.remove(query, EmbeddingDocument.class);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId).and("documentId").in(documentIds));
        mongoTemplate.remove(query, EmbeddingDocument.class);
        log.debug("Deleted embeddings from MongoDB by document IDs: {}", documentIds);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId));
        mongoTemplate.remove(query, EmbeddingDocument.class);
        log.debug("Deleted embeddings from MongoDB for knowledge ID: {}", knowledgeId);
    }

    /**
     * 按来源类型做全文检索：仅依据 {@link SearchRequest} 入参构造过滤条件，
     * 不再自行解析排除段落（该策略已上移到 {@link com.maxkb4j.knowledge.retriever.SearchOrchestrator}）。
     * <p>段落路按 excludeDocumentIds / excludeParagraphIds 过滤；问题路不应用这两类排除
     * （sourceId 为 problemId，与段落无关）。</p>
     */
    @Override
    public List<TextChunkVO> searchBySource(SearchRequest request, int sourceType) {
        if (shouldShortCircuit(request)) {
            return Collections.emptyList();
        }
        Criteria baseCriteria = Criteria.where("knowledgeId").in(request.getKnowledgeIds())
                .and("sourceType").is(sourceType);
        if (sourceType == SourceType.PARAGRAPH) {
            if (!CollectionUtils.isEmpty(request.getExcludeDocumentIds())) {
                baseCriteria.and("documentId").nin(request.getExcludeDocumentIds());
            }
            if (!CollectionUtils.isEmpty(request.getExcludeParagraphIds())) {
                baseCriteria.and("sourceId").nin(request.getExcludeParagraphIds());
            }
        }
        return searchByCriteria(request, baseCriteria);
    }

    private List<TextChunkVO> searchByCriteria(SearchRequest request, Criteria baseCriteria) {
        try {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                    .matching(Tokenizer.segment(request.getQuery()));
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(textCriteria),
                    Aggregation.match(baseCriteria),
                    Aggregation.addFields()
                            .addField("score")
                            .withValueOf(MongoExpression.create("{$meta: 'textScore'}"))
                            .build(),
                    Aggregation.sort(Sort.Direction.DESC, "score"),
                    Aggregation.project("sourceId", "score"),
                    Aggregation.limit(request.getTopK())
            );
            List<EmbeddingDocument> result = mongoTemplate.aggregate(
                    aggregation,
                    mongoTemplate.getCollectionName(EmbeddingDocument.class),
                    EmbeddingDocument.class
            ).getMappedResults();
            if (CollectionUtils.isEmpty(result)) {
                return Collections.emptyList();
            }
            double topScore = result.getFirst() == null ? 0 : result.getFirst().getScore();
            double maxScore = Math.max(topScore, SCORE_NORMALIZE_CEILING);
            return result.stream()
                    .map(entity -> new TextChunkVO(entity.getSourceId(), entity.getScore() / maxScore))
                    .filter(vo -> vo.getScore() >= request.getMinScore())
                    .toList();
        } catch (Exception e) {
            log.error("Full-text search failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

}