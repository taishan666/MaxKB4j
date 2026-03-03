package com.tarzan.maxkb4j.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.common.util.BatchUtil;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.module.knowledge.service.IChunkIndexService;
import com.tarzan.maxkb4j.module.knowledge.util.TextSegmentUtil;
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

@Component
@RequiredArgsConstructor
public class PgVectorIndexService implements IChunkIndexService {

    private final MongoTemplate mongoTemplate;
    private final EmbeddingMapper embeddingMapper;


    @Override
    public void insertAll( EmbeddingModel embeddingModel,List<EmbeddingEntity> embeddingEntities) {
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
                embeddingEntity.setContent(TextSegmentUtil.segment(embeddingEntity.getContent()));
                embeddingEntity.setDimension(embeddingModel.dimension());
            }
        });
        embeddingMapper.insert(validEntities);
        mongoTemplate.insertAll(validEntities);
    }

    @Override
    public void updateActiveByParagraphId(String knowledgeId,String paragraphId, Boolean isActive) {
        embeddingMapper.updateActiveByParagraphId(knowledgeId,paragraphId,Boolean.TRUE.equals(isActive));
        // 创建查询条件，匹配 paragraphId
        Query query = new Query(Criteria.where("paragraphId").is(paragraphId));
        // 创建更新对象，设置 IsActive 字段的新值
        Update update = new Update().set("isActive", Boolean.TRUE.equals(isActive));
        // 使用 MongoTemplate 执行更新操作
        mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
    }

    @Override
    public void removeByProblemIdAndParagraphId(String knowledgeId,String problemId, String paragraphId) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(EmbeddingEntity::getParagraphId,paragraphId);
        queryWrapper.eq(EmbeddingEntity::getSourceId,problemId);
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId,knowledgeId);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("paragraphId").is(paragraphId).and("sourceId").is(problemId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }


    @Override
    public void removeByParagraphIds(String knowledgeId,List<String> paragraphIds) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(EmbeddingEntity::getParagraphId,paragraphIds);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("paragraphId").in(paragraphIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    @Override
    public void removeByDocIds(String knowledgeId,List<String> docIds) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(EmbeddingEntity::getDocumentId,docIds);
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId,knowledgeId);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("documentId").in(docIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    @Override
    public void removeBySourceIds(String knowledgeId,List<String> sourceIds) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(EmbeddingEntity::getSourceId,sourceIds);
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId,knowledgeId);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("sourceId").in(sourceIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    @Override
    public void removeByKnowledgeId(String knowledgeId) {
        LambdaQueryWrapper<EmbeddingEntity>  queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(EmbeddingEntity::getKnowledgeId,knowledgeId);
        embeddingMapper.delete(queryWrapper);
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }



}
