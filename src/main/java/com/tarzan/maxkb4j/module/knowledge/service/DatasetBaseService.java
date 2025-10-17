package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DatasetBaseService {
    private final KnowledgeMapper datasetMapper;
    private final ModelFactory modelFactory;

    @Cacheable(cacheNames = "dataset_embedding_model", key = "#knowledgeId")
    public EmbeddingModel getDatasetEmbeddingModel(String knowledgeId){
        KnowledgeEntity dataset=datasetMapper.selectById(knowledgeId);
        return modelFactory.build(dataset.getEmbeddingModelId());
    }

    public KnowledgeEntity getById(String knowledgeId) {
        return datasetMapper.selectById(knowledgeId);
    }
}
