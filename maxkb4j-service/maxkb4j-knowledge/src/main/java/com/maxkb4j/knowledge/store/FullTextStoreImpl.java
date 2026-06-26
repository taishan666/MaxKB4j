package com.maxkb4j.knowledge.store;

import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.knowledge.util.Tokenizer;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
     * <p>MongoDB textScore 由 BM25 加权累加得到，没有理论上界；实际观测下绝大多数命中
     * 都落在 [0, 2] 区间。这里取 max(observedMax, 2.0) 兜底，保证 score 落在 [0, 1]
     * 区间，避免极端值（如单段命中数百次关键词）把整体分布拉爆。</p>
     */
    private static final double SCORE_NORMALIZE_CEILING = 2.0;

    private final MongoTemplate mongoTemplate;
    /** 延迟解析以避免与 ParagraphService 之间的构造期循环依赖，仅在 search() 中使用。 */
    private final ObjectProvider<IParagraphService> paragraphServiceProvider;

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        entities.forEach(entity -> {
            String content = Tokenizer.segment(entity.getContent());
            entity.setContent(content);
        });
        mongoTemplate.insertAll(entities);
        log.debug("Inserted {} embedding entities into MongoDB", entities.size());
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId)
                .and("paragraphId").is(paragraphId)
                .and("sourceId").is(problemId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId)
                .and("sourceType").is(SourceType.PROBLEM)
                .and("sourceId").in(problemIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (paragraphIds == null || paragraphIds.isEmpty()) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId).and("paragraphId").in(paragraphIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
        log.debug("Deleted embeddings from MongoDB by paragraph IDs: {}", paragraphIds);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId).and("documentId").in(documentIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
        log.debug("Deleted embeddings from MongoDB by document IDs: {}", documentIds);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
        log.debug("Deleted embeddings from MongoDB for knowledge ID: {}", knowledgeId);
    }

    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (shouldShortCircuit(request)) {
            return Collections.emptyList();
        }
        try {
            List<String> excludeParagraphIds = resolveExcludeParagraphIds(request, paragraphServiceProvider.getObject());
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                    .matching(Tokenizer.segment(request.getQuery()));
            Criteria baseCriteria = Criteria.where("knowledgeId").in(request.getKnowledgeIds());
            if (!CollectionUtils.isEmpty(request.getExcludeDocumentIds())) {
                baseCriteria.and("documentId").nin(request.getExcludeDocumentIds());
            }
            if (!CollectionUtils.isEmpty(excludeParagraphIds)) {
                baseCriteria.and("paragraphId").nin(excludeParagraphIds);
            }
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(textCriteria),
                    Aggregation.match(baseCriteria),
                    Aggregation.addFields()
                            .addField("score")
                            .withValueOf(MongoExpression.create("{$meta: 'textScore'}"))
                            .build(),
                    Aggregation.sort(Sort.Direction.DESC, "score"),
                    Aggregation.group("paragraphId").first("$$ROOT").as("highestScoreDoc"),
                    Aggregation.replaceRoot("highestScoreDoc"),
                    Aggregation.sort(Sort.Direction.DESC, "score"),
                    Aggregation.limit(request.getTopK())
            );
            List<EmbeddingEntity> result = mongoTemplate.aggregate(
                    aggregation,
                    mongoTemplate.getCollectionName(EmbeddingEntity.class),
                    EmbeddingEntity.class
            ).getMappedResults();
            if (CollectionUtils.isEmpty(result)) {
                return Collections.emptyList();
            }
            double topScore = result.getFirst() == null ? 0 : result.getFirst().getScore();
            double maxScore = Math.max(topScore, SCORE_NORMALIZE_CEILING);
            return result.stream()
                    .map(entity -> new TextChunkVO(entity.getParagraphId(), entity.getScore() / maxScore))
                    .filter(vo -> vo.getScore() >= request.getMinScore())
                    .toList();
        } catch (Exception e) {
            log.error("Full-text search failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
