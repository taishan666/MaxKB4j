package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class KnowledgeModelService {

    private final IModelProviderService modelFactory;
    private final KnowledgeMapper knowledgeMapper;

    public EmbeddingModel getEmbeddingModel(String knowledgeId){
        LambdaQueryWrapper<KnowledgeEntity> wrapper=Wrappers.<KnowledgeEntity>lambdaQuery()
                .select(KnowledgeEntity::getEmbeddingModelId)
                .eq(KnowledgeEntity::getId,knowledgeId);
        KnowledgeEntity knowledge=knowledgeMapper.selectOne(wrapper);
        if (knowledge==null){
            throw new RuntimeException("数据集不存在");
        }
        return modelFactory.buildEmbeddingModel(knowledge.getEmbeddingModelId());
    }
}
