package com.tarzan.maxkb4j.module.knowledge.service;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.langchain4j.EmbeddingStoreFactory;
import com.tarzan.maxkb4j.module.knowledge.consts.SourceType;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.EmbeddingMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
public class DataIndexService {

    private final MongoTemplate mongoTemplate;
    private final EmbeddingStoreFactory embeddingStoreFactory;
    private final EmbeddingMapper embeddingMapper;


    public void insertAll(List<EmbeddingEntity> embeddingEntities, EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        List<Embedding> embeddings=new ArrayList<>();
        List<TextSegment> textSegments=new ArrayList<>();
        for (EmbeddingEntity embeddingEntity : embeddingEntities) {
            Response<Embedding> res = embeddingModel.embed(embeddingEntity.getContent());
            embeddingEntity.setContent(segmentContent(embeddingEntity.getContent()));
            embeddings.add(res.content());
            textSegments.add(embeddingEntity.toTextSegment());
        }
        embeddingStore.addAll(embeddings,textSegments);
        embeddingMapper.insert(embeddingEntities);
        mongoTemplate.insertAll(embeddingEntities);
    }


    //todo
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
        EmbeddingStore<TextSegment>  embeddingStore =embeddingStoreFactory.get(knowledgeId);
        Filter filter = MetadataFilterBuilder
                .metadataKey("source_id").isEqualTo(problemId).and(MetadataFilterBuilder.metadataKey("paragraph_id").isEqualTo(paragraphId));
        embeddingStore.removeAll(filter);
        Query query = new Query(Criteria.where("paragraphId").is(paragraphId).and("sourceId").is(problemId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    public void removeByParagraphId(String knowledgeId,String paragraphId) {
        removeByParagraphIds(knowledgeId,List.of(paragraphId));
    }

    public void removeByParagraphIds(String knowledgeId,List<String> paragraphIds) {
        EmbeddingStore<TextSegment>  embeddingStore =embeddingStoreFactory.get(knowledgeId);
        Filter filter = MetadataFilterBuilder
                .metadataKey("paragraph_id").isIn(paragraphIds);
        embeddingStore.removeAll(filter);
        Query query = new Query(Criteria.where("paragraphId").in(paragraphIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    public void removeByDocIds(String knowledgeId,List<String> docIds) {
        EmbeddingStore<TextSegment>  embeddingStore =embeddingStoreFactory.get(knowledgeId);
        Filter filter = MetadataFilterBuilder
                .metadataKey("document_id").isIn(docIds);
        embeddingStore.removeAll(filter);
        Query query = new Query(Criteria.where("documentId").in(docIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }


    public void removeBySourceIds(String knowledgeId,List<String> sourceIds) {
        EmbeddingStore<TextSegment>  embeddingStore =embeddingStoreFactory.get(knowledgeId);
        Filter filter = MetadataFilterBuilder
                .metadataKey("source_id").isIn(sourceIds);
        embeddingStore.removeAll(filter);
        Query query = new Query(Criteria.where("sourceId").in(sourceIds));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    public void removeByDatasetId(String knowledgeId) {
        embeddingStoreFactory.drop(knowledgeId);
        Query query = new Query(Criteria.where("knowledgeId").is(knowledgeId));
        mongoTemplate.remove(query, EmbeddingEntity.class);
    }

    public String segmentContent(String text) {
        if (StringUtil.isBlank(text)) {
            return StringUtil.EMPTY;
        }
        JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();
        List<String> tokens = jiebaSegmenter.sentenceProcess(text);
        return String.join(" ", tokens);
    }

    public void migrateDoc(String targetKnowledgeId, List<String> docIds) {
        //embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate().set(EmbeddingEntity::getKnowledgeId, targetKnowledgeId).eq(EmbeddingEntity::getSourceType, SourceType.PARAGRAPH).in(EmbeddingEntity::getDocumentId, docIds));
        //embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaUpdate().eq(EmbeddingEntity::getSourceType, SourceType.PROBLEM).in(EmbeddingEntity::getDocumentId, docIds));
        Query query = new Query(Criteria.where("documentId").in(docIds).and("sourceType").is(SourceType.PARAGRAPH));
        Update update = new Update().set("knowledgeId", targetKnowledgeId);
        mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
        mongoTemplate.remove(new Query(Criteria.where("documentId").in(docIds).and("sourceType").is(SourceType.PROBLEM)), EmbeddingEntity.class);
    }


    public void migrateParagraph(String sourceKnowledgeId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds) {
        if (sourceKnowledgeId.equals(targetKnowledgeId)) {
      /*      embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate()
                    .set(EmbeddingEntity::getKnowledgeId, targetKnowledgeId)
                    .set(EmbeddingEntity::getDocumentId, targetDocId)
                    .in(EmbeddingEntity::getParagraphId, paragraphIds));*/
            Query query = new Query(Criteria.where("paragraphId").in(paragraphIds));
            Update update = new Update().set("knowledgeId", targetKnowledgeId).set("documentId", targetDocId);
            mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
        } else {
/*         //   embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate()
                    .set(EmbeddingEntity::getKnowledgeId, targetKnowledgeId)
                    .set(EmbeddingEntity::getDocumentId, targetDocId)
                    .eq(EmbeddingEntity::getSourceType, SourceType.PARAGRAPH)
                    .in(EmbeddingEntity::getParagraphId, paragraphIds));*/
         //   embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaUpdate().eq(EmbeddingEntity::getSourceType, SourceType.PROBLEM).in(EmbeddingEntity::getParagraphId, paragraphIds));
            Query query = new Query(Criteria.where("paragraphId").in(paragraphIds).and("sourceType").is(SourceType.PARAGRAPH));
            Update update = new Update().set("knowledgeId", targetKnowledgeId).set("documentId", targetDocId);
            mongoTemplate.updateMulti(query, update, EmbeddingEntity.class);
            mongoTemplate.remove(new Query(Criteria.where("paragraphId").in(paragraphIds).and("sourceType").is(SourceType.PROBLEM)), EmbeddingEntity.class);
        }
    }

}
