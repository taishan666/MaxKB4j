package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.module.dataset.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.util.StringUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
public class DataIndexService {

    private final MongoTemplate mongoTemplate;
    private final EmbeddingMapper embeddingMapper;


    public void insertAll(List<EmbeddingEntity> embeddings) {
        embeddingMapper.insert(embeddings);
        for (EmbeddingEntity embedding : embeddings) {
            embedding.setContent(segmentContent(embedding.getContent()));
        }
        mongoTemplate.insertAll(embeddings);
    }

    public void updateActiveByParagraph(ParagraphEntity paragraph) {
        if (Objects.nonNull(paragraph.getIsActive())) {
            embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate().set(EmbeddingEntity::getIsActive, paragraph.getIsActive()).eq(EmbeddingEntity::getParagraphId, paragraph.getId()));
            // 创建查询条件，匹配 paragraphId
            Query query = new Query(Criteria.where("paragraphId").is(paragraph.getId()));
            // 创建更新对象，设置 IsActive 字段的新值
            Update update = new Update().set("IsActive", paragraph.getIsActive());
            // 使用 MongoTemplate 执行更新操作
            mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
        }
    }

    public void removeByParagraphId(String paragraphId) {
        removeByParagraphIds(List.of(paragraphId));
    }

    public void removeByParagraphIds(List<String> paragraphIds) {
        embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaQuery().in(EmbeddingEntity::getParagraphId, paragraphIds));
        Query query = new Query(Criteria.where("paragraphId").in(paragraphIds));
        mongoTemplate.remove(query,EmbeddingEntity.class);
    }

    public void removeByDocIds(List<String> docIds) {
        embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaQuery().in(EmbeddingEntity::getDocumentId, docIds));
        Query query = new Query(Criteria.where("documentId").in(docIds));
        mongoTemplate.remove(query,EmbeddingEntity.class);
    }

    public void removeBySourceId(String sourceId) {
        embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaQuery().eq(EmbeddingEntity::getSourceId, sourceId));
        Query query = new Query(Criteria.where("sourceId").is(sourceId));
        mongoTemplate.remove(query,EmbeddingEntity.class);
    }

    public void removeBySourceIds(List<String> sourceIds) {
        embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaQuery().in(EmbeddingEntity::getSourceId, sourceIds));
        Query query = new Query(Criteria.where("sourceId").in(sourceIds));
        mongoTemplate.remove(query,EmbeddingEntity.class);
    }

    public void removeByDatasetId(String datasetId) {
        embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaQuery().eq(EmbeddingEntity::getDatasetId, datasetId));
        Query query = new Query(Criteria.where("datasetId").is(datasetId));
        mongoTemplate.remove(query,EmbeddingEntity.class);
    }

    public String segmentContent(String text) {
        if (StringUtil.isBlank(text)){
            return StringUtil.EMPTY;
        }
        JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();
        List<String> tokens = jiebaSegmenter.sentenceProcess(text);
        return String.join(" ", tokens);
    }

    public void migrateDoc(String targetId, List<String> docIds) {
        embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate().set(EmbeddingEntity::getDatasetId, targetId).in(EmbeddingEntity::getDocumentId,docIds));
        // 创建查询条件，匹配 paragraphId
        Query query = new Query(Criteria.where("documentId").is(docIds));
        // 创建更新对象，设置 IsActive 字段的新值
        Update update = new Update().set("datasetId", targetId);
        // 使用 MongoTemplate 执行更新操作
        mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
    }
}
