package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

public interface IChunkIndexService {

    void insertAll(List<EmbeddingEntity> embeddingEntities, EmbeddingModel embeddingModel);

    void updateActiveByParagraphId(String knowledgeId, ParagraphEntity paragraph);

    void removeByProblemIdAndParagraphId(String knowledgeId,String problemId, String paragraphId);
    default void removeByParagraphId(String paragraphId) {
        removeByParagraphIds(List.of(paragraphId));
    }

    void removeByParagraphIds(List<String> paragraphIds);

    void removeByDocIds(String knowledgeId,List<String> docIds);

    void removeBySourceIds(String knowledgeId,List<String> sourceIds);

    void removeByKnowledgeId(String knowledgeId);

    void migrateDoc(String targetKnowledgeId, List<String> docIds);

    void migrateParagraph(String sourceKnowledgeId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds);
}
