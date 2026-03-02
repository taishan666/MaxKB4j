/*
package com.tarzan.maxkb4j.module.knowledge.service.impl;

import com.tarzan.maxkb4j.common.util.BatchUtil;
import com.tarzan.maxkb4j.module.knowledge.consts.SourceType;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.service.IChunkIndexService;
import com.tarzan.maxkb4j.module.knowledge.util.TextSegmentUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

*/
/**
 * Elasticsearch 索引服务
 * 负责向量和全文索引的 CRUD 操作
 *
 * @author tarzan
 * @date 2026-03-02
 *//*

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchService implements IChunkIndexService {

    private final ElasticsearchOperations elasticsearchOperations;

    private static final String INDEX_NAME = "embedding";

    @Override
    @Transactional
    public void insertAll(List<EmbeddingEntity> embeddingEntities, EmbeddingModel embeddingModel) {
        List<EmbeddingEntity> validEntities = embeddingEntities.stream()
                .filter(e -> e != null && StringUtils.isNotBlank(e.getContent()))
                .toList();
        BatchUtil.protectBach(validEntities, 3, batch -> {
            List<TextSegment> textSegments = batch.stream().map(e -> TextSegment.from(e.getContent())).toList();
            Response<List<Embedding>> res = embeddingModel.embedAll(textSegments);
            List<Embedding> embeddings = res.content();
            for (int i = 0; i < batch.size(); i++) {
                Embedding embedding = embeddings.get(i);
                EmbeddingEntity embeddingEntity = batch.get(i);
                embeddingEntity.setEmbedding(embedding.vectorAsList());
                embeddingEntity.setContent(TextSegmentUtil.segment(embeddingEntity.getContent()));
                embeddingEntity.setDimension(embeddingModel.dimension());
            }
        });

        if (!validEntities.isEmpty()) {
            elasticsearchOperations.save(validEntities, IndexCoordinates.of(INDEX_NAME));
        }
    }

    @Override
    public void updateActiveByParagraphId(String knowledgeId, ParagraphEntity paragraph) {
        String paragraphId = paragraph.getId();
        Boolean isActive = paragraph.getIsActive();
        Document document = Document.create();
        document.put("isActive", isActive);
        UpdateQuery updateQuery = UpdateQuery.builder(paragraphId)
                .withDocument(document)
                .build();
        elasticsearchOperations.update(updateQuery, IndexCoordinates.of(INDEX_NAME));
        log.info("更新段落 [{}] 的激活状态为：{}", paragraphId, isActive);
    }

    @Override
    public void removeByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        Query query = new CriteriaQuery(
                Criteria.where("paragraphId").is(paragraphId).and("sourceId").is(problemId)
        );
        elasticsearchOperations.delete(DeleteQuery.builder(query).build(), EmbeddingEntity.class);
        log.info("删除问题 [{}] 和段落 [{}] 的关联索引", problemId, paragraphId);
    }

    @Override
    public void removeByParagraphIds(List<String> paragraphIds) {
        if (paragraphIds.isEmpty()) {
            return;
        }
        Query query = new CriteriaQuery(Criteria.where("paragraphId").in(paragraphIds));
        elasticsearchOperations.delete(DeleteQuery.builder(query).build(), EmbeddingEntity.class);
        log.info("批量删除 {} 个段落的索引", paragraphIds.size());
    }

    @Override
    public void removeByDocIds(String knowledgeId, List<String> docIds) {
        if (docIds.isEmpty()) {
            return;
        }
        Query query = new CriteriaQuery(
                Criteria.where("documentId").in(docIds).and("knowledgeId").is(knowledgeId)
        );
        elasticsearchOperations.delete(DeleteQuery.builder(query).build(), EmbeddingEntity.class);
        log.info("批量删除 {} 个文档的索引", docIds.size());
    }

    @Override
    public void removeBySourceIds(String knowledgeId, List<String> sourceIds) {
        if (sourceIds.isEmpty()) {
            return;
        }
        Query query = new CriteriaQuery(
                Criteria.where("sourceId").in(sourceIds).and("knowledgeId").is(knowledgeId)
        );
        elasticsearchOperations.delete(DeleteQuery.builder(query).build(), EmbeddingEntity.class);
        log.info("批量删除 {} 个源 ID 的索引", sourceIds.size());
    }

    @Override
    public void removeByKnowledgeId(String knowledgeId) {
        Query query = new CriteriaQuery(Criteria.where("knowledgeId").is(knowledgeId));
        elasticsearchOperations.delete(DeleteQuery.builder(query).build(), EmbeddingEntity.class);
        log.info("删除知识库 [{}] 的所有索引", knowledgeId);
    }

    @Override
    public void migrateDoc(String targetKnowledgeId, List<String> docIds) {
        if (docIds.isEmpty()) {
            return;
        }

        Criteria paragraphCriteria = Criteria.where("documentId").in(docIds)
                .and("sourceType").is(SourceType.PARAGRAPH);
        List<EmbeddingEntity> paragraphEntities = elasticsearchOperations.search(
                new CriteriaQuery(paragraphCriteria),
                EmbeddingEntity.class
        ).stream().map(SearchHit::getContent).toList();

        for (EmbeddingEntity entity : paragraphEntities) {
            entity.setKnowledgeId(targetKnowledgeId);
            elasticsearchOperations.save(entity);
        }

        Criteria problemCriteria = Criteria.where("documentId").in(docIds)
                .and("sourceType").is(SourceType.PROBLEM);
        Query problemQuery = new CriteriaQuery(problemCriteria);
        elasticsearchOperations.delete(DeleteQuery.builder(problemQuery).build(), EmbeddingEntity.class);
        log.info("迁移 {} 个文档到知识库 [{}]", docIds.size(), targetKnowledgeId);
    }

    @Override
    public void migrateParagraph(String sourceKnowledgeId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds) {
        if (paragraphIds.isEmpty()) {
            return;
        }

        if (sourceKnowledgeId.equals(targetKnowledgeId)) {
            Criteria criteria = Criteria.where("paragraphId").in(paragraphIds);
            List<EmbeddingEntity> entities = elasticsearchOperations.search(
                    new CriteriaQuery(criteria),
                    EmbeddingEntity.class
            ).stream().map(SearchHit::getContent).toList();

            for (EmbeddingEntity entity : entities) {
                entity.setKnowledgeId(targetKnowledgeId);
                entity.setDocumentId(targetDocId);
                elasticsearchOperations.save(entity);
            }
        } else {
            Criteria paragraphCriteria = Criteria.where("paragraphId").in(paragraphIds)
                    .and("sourceType").is(SourceType.PARAGRAPH);
            List<EmbeddingEntity> paragraphEntities = elasticsearchOperations.search(
                    new CriteriaQuery(paragraphCriteria),
                    EmbeddingEntity.class
            ).stream().map(SearchHit::getContent).toList();

            for (EmbeddingEntity entity : paragraphEntities) {
                entity.setKnowledgeId(targetKnowledgeId);
                entity.setDocumentId(targetDocId);
                elasticsearchOperations.save(entity);
            }

            Criteria problemCriteria = Criteria.where("paragraphId").in(paragraphIds)
                    .and("sourceType").is(SourceType.PROBLEM);
            Query problemQuery = new CriteriaQuery(problemCriteria);
            elasticsearchOperations.delete(DeleteQuery.builder(problemQuery).build(), EmbeddingEntity.class);
        }

        log.info("迁移 {} 个段落从知识库 [{}] 到 [{}]", paragraphIds.size(), sourceKnowledgeId, targetKnowledgeId);
    }
}
*/
