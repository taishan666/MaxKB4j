package com.tarzan.maxkb4j.module.dataset.service;

import com.tarzan.maxkb4j.module.dataset.domain.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.DatasetMapper;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DatasetBaseService {
    private final DatasetMapper datasetMapper;
    private final ModelService modelService;

    @Cacheable(cacheNames = "dataset_embedding_model", key = "#datasetId")
    public EmbeddingModel getDatasetEmbeddingModel(String datasetId){
        DatasetEntity dataset=datasetMapper.selectById(datasetId);
        return modelService.getModelById(dataset.getEmbeddingModelId());
    }

    public DatasetEntity getById(String datasetId) {
        return datasetMapper.selectById(datasetId);
    }
}
