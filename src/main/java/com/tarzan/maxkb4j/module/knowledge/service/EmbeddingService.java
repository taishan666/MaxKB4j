package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Slf4j
@Service
@AllArgsConstructor
public class EmbeddingService extends ServiceImpl<EmbeddingMapper, EmbeddingEntity> {

    private final ProblemMapper problemMapper;
    private final DataIndexService dataIndexService;


    public boolean createProblemIndex(String knowledgeId, String docId, String paragraphId, String problemId, EmbeddingModel embeddingModel) {
        ProblemEntity problem = problemMapper.selectById(problemId);
        if (Objects.nonNull(problem)) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setKnowledgeId(knowledgeId);
            embeddingEntity.setDocumentId(docId);
            embeddingEntity.setParagraphId(paragraphId);
            embeddingEntity.setMeta(new JSONObject());
            embeddingEntity.setSourceId(problemId);
            embeddingEntity.setSourceType("0");
            embeddingEntity.setIsActive(true);
            Response<Embedding> res = embeddingModel.embed(problem.getContent());
            embeddingEntity.setEmbedding(res.content().vectorAsList());
            embeddingEntity.setContent(problem.getContent());
            dataIndexService.insertAll(List.of(embeddingEntity));
            return true;
        }
        return false;
    }

}
