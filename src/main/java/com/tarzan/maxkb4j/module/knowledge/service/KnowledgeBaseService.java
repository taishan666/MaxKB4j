package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    private final KnowledgeMapper datasetMapper;
    private final ModelFactory modelFactory;

    @Cacheable(cacheNames = "dataset_embedding_model", key = "#knowledgeId")
    public EmbeddingModel getEmbeddingModel(String knowledgeId){
        KnowledgeEntity dataset=datasetMapper.selectById(knowledgeId);
        if (dataset==null){
            throw new RuntimeException("数据集不存在");
        }
        return modelFactory.buildEmbeddingModel(dataset.getEmbeddingModelId());
    }

    public KnowledgeEntity getById(String knowledgeId) {
        return datasetMapper.selectById(knowledgeId);
    }
}
