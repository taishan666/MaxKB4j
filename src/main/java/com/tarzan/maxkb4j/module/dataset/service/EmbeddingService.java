package com.tarzan.maxkb4j.module.dataset.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.domain.vo.ProblemParagraphVO;
import com.tarzan.maxkb4j.module.dataset.domain.vo.TextChunkVO;
import com.tarzan.maxkb4j.module.dataset.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemParagraphMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Slf4j
@Service
@AllArgsConstructor
public class EmbeddingService extends ServiceImpl<EmbeddingMapper, EmbeddingEntity> {

    private final ParagraphMapper paragraphMapper;
    private final ProblemParagraphMapper problemParagraphMappingMapper;
    private final ProblemMapper problemMapper;
    private final DataIndexService dataIndexService;


    @Transactional
    public boolean embedProblemParagraphs(List<ParagraphEntity> paragraphs, List<ProblemParagraphVO> problemParagraphs, EmbeddingModel embeddingModel) {
        List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
        for (ParagraphEntity paragraph : paragraphs) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setDatasetId(paragraph.getDatasetId());
            embeddingEntity.setDocumentId(paragraph.getDocumentId());
            embeddingEntity.setParagraphId(paragraph.getId());
            embeddingEntity.setMeta(new JSONObject());
            embeddingEntity.setSourceId(paragraph.getId());
            embeddingEntity.setSourceType("1");
            embeddingEntity.setIsActive(paragraph.getIsActive());
            embeddingModel.embed(paragraph.getTitle() + paragraph.getContent());
            Response<Embedding> res;
            if (StringUtil.isNotBlank(paragraph.getTitle())) {
                res = embeddingModel.embed(paragraph.getTitle() + paragraph.getContent());
            } else {
                res = embeddingModel.embed(paragraph.getContent());
            }
            embeddingEntity.setEmbedding(res.content().vectorAsList());
            embeddingEntities.add(embeddingEntity);
        }
        if (!CollectionUtils.isEmpty(problemParagraphs)) {
            Map<String, List<ProblemParagraphVO>> map = problemParagraphs.stream().collect(Collectors.groupingBy(ProblemParagraphVO::getProblemId));
            map.forEach((k, v) -> {
                Response<Embedding> res = embeddingModel.embed(v.get(0).getContent());
                v.forEach(pp -> {
                    EmbeddingEntity embeddingEntity = new EmbeddingEntity();
                    embeddingEntity.setDatasetId(pp.getDatasetId());
                    embeddingEntity.setDocumentId(pp.getDocumentId());
                    embeddingEntity.setParagraphId(pp.getParagraphId());
                    embeddingEntity.setMeta(new JSONObject());
                    embeddingEntity.setSourceId(pp.getProblemId());
                    embeddingEntity.setSourceType("0");
                    embeddingEntity.setIsActive(true);
                    embeddingEntity.setEmbedding(res.content().vectorAsList());
                    embeddingEntity.setContent(v.get(0).getContent());
                    embeddingEntities.add(embeddingEntity);
                });
            });
        }
        dataIndexService.insertAll(embeddingEntities);
        return true;
    }

    public boolean createProblem(String datasetId, String docId, String paragraphId, String problemId, EmbeddingModel embeddingModel) {
        ProblemEntity problem = problemMapper.selectById(problemId);
        if (Objects.nonNull(problem)) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setDatasetId(datasetId);
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


    @Transactional
    public boolean embedByDatasetId(String datasetId, EmbeddingModel embeddingModel) {
        this.lambdaUpdate().in(EmbeddingEntity::getDatasetId, datasetId).remove();
        List<ParagraphEntity> paragraphEntities = paragraphMapper.selectList(Wrappers.<ParagraphEntity>lambdaQuery().in(ParagraphEntity::getDatasetId, datasetId));
        List<ProblemParagraphVO> problemParagraphVOS = problemParagraphMappingMapper.getProblems(datasetId, null);
        return embedProblemParagraphs(paragraphEntities, problemParagraphVOS, embeddingModel);
    }

    public List<TextChunkVO> embeddingSearch(List<String> datasetIds, List<String> excludeParagraphIds, int maxResults, double minScore, float[] referenceEmbedding) {
        return baseMapper.embeddingSearch(datasetIds, excludeParagraphIds, maxResults, minScore, referenceEmbedding);
    }
}
