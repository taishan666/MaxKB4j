package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class knowledgeModelService {

    private final ModelFactory modelFactory;
    private final KnowledgeMapper knowledgeMapper;

    @Cacheable(cacheNames = "dataset_embedding_model", key = "#knowledgeId")
    public EmbeddingModel getEmbeddingModel(String knowledgeId){
        KnowledgeEntity dataset=knowledgeMapper.selectById(knowledgeId);
        if (dataset==null){
            throw new RuntimeException("数据集不存在");
        }
        return modelFactory.buildEmbeddingModel(dataset.getEmbeddingModelId());
    }
}
