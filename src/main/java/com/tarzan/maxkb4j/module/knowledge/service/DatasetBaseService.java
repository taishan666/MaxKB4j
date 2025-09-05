package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DatasetBaseService {
    private final KnowledgeMapper datasetMapper;
    private final ModelService modelService;

    @Cacheable(cacheNames = "dataset_embedding_model", key = "#datasetId")
    public EmbeddingModel getDatasetEmbeddingModel(String datasetId){
        KnowledgeEntity dataset=datasetMapper.selectById(datasetId);
        return modelService.getModelById(dataset.getEmbeddingModelId());
    }

    public KnowledgeEntity getById(String datasetId) {
        return datasetMapper.selectById(datasetId);
    }
}
