package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ElasticsearchService implements IChunkIndexService{

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void insertAll(List<EmbeddingEntity> embeddingEntities, EmbeddingModel embeddingModel) {

    }

    @Override
    public void updateActiveByParagraphId(String knowledgeId, ParagraphEntity paragraph) {

    }

    @Override
    public void removeByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {

    }

    @Override
    public void removeByParagraphIds(List<String> paragraphIds) {

    }

    @Override
    public void removeByDocIds(String knowledgeId, List<String> docIds) {

    }

    @Override
    public void removeBySourceIds(String knowledgeId, List<String> sourceIds) {

    }

    @Override
    public void removeByKnowledgeId(String knowledgeId) {

    }

    @Override
    public void migrateDoc(String targetKnowledgeId, List<String> docIds) {

    }

    @Override
    public void migrateParagraph(String sourceKnowledgeId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds) {

    }
}
