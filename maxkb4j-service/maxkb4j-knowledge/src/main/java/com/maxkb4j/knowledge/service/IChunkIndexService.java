package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

public interface IChunkIndexService {

    void insertAll(EmbeddingModel embeddingModel, List<EmbeddingEntity> embeddingEntities);

    void updateActiveByParagraphId(String knowledgeId, String paragraphId, Boolean isActive);

    void removeByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId);

    default void removeByParagraphId(String knowledgeId, String paragraphId) {
        removeByParagraphIds(knowledgeId, List.of(paragraphId));
    }

    void removeByParagraphIds(String knowledgeId, List<String> paragraphIds);

    void removeByDocIds(String knowledgeId, List<String> docIds);

    void removeBySourceIds(String knowledgeId, List<String> sourceIds);

    void removeByKnowledgeId(String knowledgeId);

}
