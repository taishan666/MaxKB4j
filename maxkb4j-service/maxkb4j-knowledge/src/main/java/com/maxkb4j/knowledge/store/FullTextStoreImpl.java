package com.maxkb4j.knowledge.store;

import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.util.TextSegmentUtil;
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
import org.springframework.data.mongodb.core.query.Update;
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
public class FullTextStoreImpl implements IDataStore {


    private final MongoTemplate mongoTemplate;

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        // Insert into MongoDB for full-text search
        mongoTemplate.insertAll(entities);
        log.debug("Inserted {} embedding entities into MongoDB", entities.size());
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId).and("paragraphId").is(paragraphId).and("sourceId").is(problemId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId).and("sourceType").is(SourceType.PROBLEM).and("sourceId").in(problemIds));
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
    public void updateActiveStatus(String knowledgeId, String paragraphId, boolean isActive) {
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId).and("paragraphId").is(paragraphId));
        Update update = new Update().set("isActive", isActive);
        mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
        log.debug("Updated active status in MongoDB for paragraph: {} to {}", paragraphId, isActive);
    }

    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (request.getKnowledgeIds() == null || request.getKnowledgeIds().isEmpty()) {
            return Collections.emptyList();
        }
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return Collections.emptyList();
        }
        try {
            // Create text criteria for full-text search
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                    .matching(TextSegmentUtil.segment(request.getQuery()));
            Criteria baseCriteria = Criteria.where("knowledgeId").in(request.getKnowledgeIds())
                    .and("isActive").is(true);
            if (!CollectionUtils.isEmpty(request.getExcludeDocumentIds())) {
                baseCriteria.and("documentId").nin(request.getExcludeDocumentIds());
            }
            if (!CollectionUtils.isEmpty(request.getExcludeParagraphIds())) {
                baseCriteria.and("paragraphId").nin(request.getExcludeParagraphIds());
            }
            // Build aggregation pipeline for efficient full-text search
            Aggregation aggregation = Aggregation.newAggregation(
                    // Step 1: Apply text search criteria
                    Aggregation.match(textCriteria),
                    // Step 2: Apply base filter criteria
                    Aggregation.match(baseCriteria),
                    // Step 3: Add text score field
                    Aggregation.addFields()
                            .addField("score")
                            .withValueOf(MongoExpression.create("{$meta: 'textScore'}"))
                            .build(),
                    // Step 4: Sort by score descending (ensures highest score per paragraphId first)
                    Aggregation.sort(Sort.Direction.DESC, "score"),
                    // Step 5: Group by paragraphId to get highest score per paragraph
                    Aggregation.group("paragraphId")
                            .first("$$ROOT").as("highestScoreDoc"),
                    // Step 6: Promote nested document to root
                    Aggregation.replaceRoot("highestScoreDoc"),
                    // Step 7: Final sort by score
                    Aggregation.sort(Sort.Direction.DESC, "score"),
                    // Step 8: Limit results
                    Aggregation.limit(request.getTopK())
            );
            // Execute aggregation
            List<EmbeddingEntity> result = mongoTemplate.aggregate(
                    aggregation,
                    mongoTemplate.getCollectionName(EmbeddingEntity.class),
                    EmbeddingEntity.class
            ).getMappedResults();
            if (CollectionUtils.isEmpty(result)) {
                return Collections.emptyList();
            }
            // Normalize scores
            float maxScore = Math.max(result.get(0).getScore(), 2);
            for (EmbeddingEntity entity : result) {
                float normalizedScore = entity.getScore() / maxScore;
                entity.setScore(normalizedScore);
            }
            // Filter by minimum score and convert to TextChunkVO
            return result.stream()
                    .map(entity -> new TextChunkVO(entity.getParagraphId(), entity.getScore()))
                    .filter(vo -> vo.getScore() >= request.getMinScore())
                    .toList();
        } catch (Exception e) {
            log.error("Full-text search failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

}