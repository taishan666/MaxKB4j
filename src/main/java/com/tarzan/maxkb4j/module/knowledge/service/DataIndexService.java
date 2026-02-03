package com.tarzan.maxkb4j.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.common.util.BatchUtil;
import com.tarzan.maxkb4j.module.knowledge.consts.SourceType;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.EmbeddingMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DataIndexService {

    private final MongoTemplate mongoTemplate;
    private final EmbeddingMapper embeddingMapper;


    public void insertAll(List<EmbeddingEntity> embeddingEntities, EmbeddingModel embeddingModel) {
        // Filter out entities with null or blank content first
        List<EmbeddingEntity> validEntities = embeddingEntities.stream()
                .filter(e -> e != null && StringUtils.isNotBlank(e.getContent()))
                .toList();
        BatchUtil.protectBach(validEntities, 3,batch->{
            List<TextSegment> textSegments=batch.stream().map(e -> TextSegment.from(e.getContent())).toList();
            Response<List<Embedding>> res = embeddingModel.embedAll(textSegments);
            List<Embedding> embeddings = res.content();
            for (int i = 0; i < batch.size(); i++) {
                Embedding  embedding=embeddings.get(i);
                EmbeddingEntity embeddingEntity = batch.get(i);
                embeddingEntity.setEmbedding(embedding.vectorAsList());
                embeddingEntity.setContent(segmentContent(embeddingEntity.getContent()));
                embeddingEntity.setDimension(embeddingModel.dimension());
            }
        });
        embeddingMapper.insert(validEntities);
        mongoTemplate.insertAll(validEntities);
    }


    public void updateActiveByParagraphId(String knowledgeId,ParagraphEntity paragraph) {
        if (Objects.nonNull(paragraph.getIsActive())) {
            embeddingMapper.updateActiveByParagraphId(knowledgeId,paragraph.getId(),paragraph.getIsActive());
            // 创建查询条件，匹配 paragraphId
            Query query = new Query(Criteria.where("paragraphId").is(paragraph.getId()));
            // 创建更新对象，设置 IsActive 字段的新值
            Update update = new Update().set("isActive", paragraph.getIsActive());
            // 使用 MongoTemplate 执行更新操作
            mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
        }
    }

    public void removeByProblemIdAndParagraphId(String knowledgeId,String problemId, String paragraphId) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(EmbeddingEntity::getParagraphId,paragraphId);
        queryWrapper.eq(EmbeddingEntity::getSourceId,problemId);
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId,knowledgeId);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("paragraphId").is(paragraphId).and("sourceId").is(problemId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    public void removeByParagraphId(String paragraphId) {
        removeByParagraphIds(List.of(paragraphId));
    }

    public void removeByParagraphIds(List<String> paragraphIds) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(EmbeddingEntity::getParagraphId,paragraphIds);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("paragraphId").in(paragraphIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    public void removeByDocIds(String knowledgeId,List<String> docIds) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(EmbeddingEntity::getDocumentId,docIds);
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId,knowledgeId);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("documentId").in(docIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }


    public void removeBySourceIds(String knowledgeId,List<String> sourceIds) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(EmbeddingEntity::getSourceId,sourceIds);
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId,knowledgeId);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("sourceId").in(sourceIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    public void removeByDatasetId(String knowledgeId) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId,knowledgeId);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    public String segmentContent(String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();
        List<String> tokens = jiebaSegmenter.sentenceProcess(text);
        return String.join(" ", tokens);
    }

    public void migrateDoc(String targetKnowledgeId, List<String> docIds) {
        embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate().set(EmbeddingEntity::getKnowledgeId, targetKnowledgeId).eq(EmbeddingEntity::getSourceType, SourceType.PARAGRAPH).in(EmbeddingEntity::getDocumentId, docIds));
        embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaUpdate().eq(EmbeddingEntity::getSourceType, SourceType.PROBLEM).in(EmbeddingEntity::getDocumentId, docIds));
        Query query = new Query(Criteria.where("documentId").in(docIds).and("sourceType").is(SourceType.PARAGRAPH));
        Update update = new Update().set("knowledgeId", targetKnowledgeId);
        mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
        mongoTemplate.remove(new Query(Criteria.where("documentId").in(docIds).and("sourceType").is(SourceType.PROBLEM)), EmbeddingEntity.class);
    }


    public void migrateParagraph(String sourceKnowledgeId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds) {
        if (sourceKnowledgeId.equals(targetKnowledgeId)) {
            embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate()
                    .set(EmbeddingEntity::getKnowledgeId, targetKnowledgeId)
                    .set(EmbeddingEntity::getDocumentId, targetDocId)
                    .in(EmbeddingEntity::getParagraphId, paragraphIds));
            Query query = new Query(Criteria.where("paragraphId").in(paragraphIds));
            Update update = new Update().set("knowledgeId", targetKnowledgeId).set("documentId", targetDocId);
            mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
        } else {
            embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate()
                    .set(EmbeddingEntity::getKnowledgeId, targetKnowledgeId)
                    .set(EmbeddingEntity::getDocumentId, targetDocId)
                    .eq(EmbeddingEntity::getSourceType, SourceType.PARAGRAPH)
                    .in(EmbeddingEntity::getParagraphId, paragraphIds));
            embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaUpdate().eq(EmbeddingEntity::getSourceType, SourceType.PROBLEM).in(EmbeddingEntity::getParagraphId, paragraphIds));
            Query query = new Query(Criteria.where("paragraphId").in(paragraphIds).and("sourceType").is(SourceType.PARAGRAPH));
            Update update = new Update().set("knowledgeId", targetKnowledgeId).set("documentId", targetDocId);
            mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
            mongoTemplate.remove(new Query(Criteria.where("paragraphId").in(paragraphIds).and("sourceType").is(SourceType.PROBLEM)), EmbeddingEntity.class);
        }
    }

}
