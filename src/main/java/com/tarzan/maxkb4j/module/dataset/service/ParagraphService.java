package com.tarzan.maxkb4j.module.dataset.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.embedding.mapper.EmbeddingMapper;
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
import java.util.Objects;

/**
 * @author tarzan
 * @date 2024-12-27 11:13:27
 */
@Slf4j
@Service
@AllArgsConstructor
public class ParagraphService extends ServiceImpl<ParagraphMapper, ParagraphEntity>{

    private final ProblemService problemService;
    private final ProblemParagraphService problemParagraphService;
  //  private final TextSegmentService textSegmentService;
    private final EmbeddingMapper embeddingMapper;

    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusByDocIds(List.of(id),type,status,type-1,type+1);
    }

    public void updateStatusByDocIds(List<String> docIds, int type,int status)  {
        baseMapper.updateStatusByDocIds(docIds,type,status,type-1,type+1);
    }

    @Transactional
    public void migrateDoc(String sourceId, String targetId, List<String> docIds) {
        embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate().set(EmbeddingEntity::getDatasetId, targetId).eq(EmbeddingEntity::getDatasetId, sourceId));
        this.lambdaUpdate().set(ParagraphEntity::getDatasetId, targetId).eq(ParagraphEntity::getDatasetId, sourceId).update();
        problemService.lambdaUpdate().set(ProblemEntity::getDatasetId, targetId).eq(ProblemEntity::getDatasetId, sourceId).update();
        problemParagraphService.lambdaUpdate().set(ProblemParagraphEntity::getDatasetId, targetId).eq(ProblemParagraphEntity::getDatasetId, sourceId).update();
    }

    @Transactional
    public void deleteByDocIds(List<String> docIds) {
        embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaQuery().in(EmbeddingEntity::getDocumentId, docIds));
        this.lambdaUpdate().in(ParagraphEntity::getDocumentId, docIds).remove();
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery().select(ProblemParagraphEntity::getProblemId).in(ProblemParagraphEntity::getDocumentId, docIds).list();
        problemParagraphService.lambdaUpdate().in(ProblemParagraphEntity::getDocumentId, docIds).remove();
        if (!CollectionUtils.isEmpty(list)) {
              problemService.removeByIds(list.stream().map(ProblemParagraphEntity::getProblemId).toList());
        }
    }


    public void embedParagraph(ParagraphEntity paragraph,EmbeddingModel embeddingModel) {
        if (paragraph != null) {
            //清除之前向量
            embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaQuery().eq(EmbeddingEntity::getDocumentId, paragraph.getId()));
            List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
            log.info("开始---->向量化段落:{}", paragraph.getId());
            EmbeddingEntity paragraphEmbed = new EmbeddingEntity();
            paragraphEmbed.setDatasetId(paragraph.getDatasetId());
            paragraphEmbed.setDocumentId(paragraph.getDocumentId());
            paragraphEmbed.setParagraphId(paragraph.getId());
            paragraphEmbed.setMeta(new JSONObject());
            paragraphEmbed.setSourceId(paragraph.getId());
            paragraphEmbed.setSourceType("1");
            paragraphEmbed.setIsActive(paragraph.getIsActive());
            paragraphEmbed.setContent(paragraph.getTitle() + paragraph.getContent());
          //  paragraphEmbed.setSearchVector(new TSVector());
            Response<Embedding> res;
            if(StringUtil.isNotBlank(paragraph.getTitle())){
                res = embeddingModel.embed(paragraph.getTitle() + paragraph.getContent());
            }else {
                res = embeddingModel.embed(paragraph.getContent());
            }
            paragraphEmbed.setEmbedding(res.content().vectorAsList());
            embeddingEntities.add(paragraphEmbed);
            List<ProblemEntity> problems=problemParagraphService.getProblemsByParagraphId(paragraph.getId());
            for (ProblemEntity problem : problems) {
                EmbeddingEntity problemEmbed = new EmbeddingEntity();
                problemEmbed.setDatasetId(paragraph.getDatasetId());
                problemEmbed.setDocumentId(paragraph.getDocumentId());
                problemEmbed.setParagraphId(paragraph.getId());
                problemEmbed.setMeta(new JSONObject());
                problemEmbed.setSourceId(problem.getId());
                paragraphEmbed.setSourceId(problem.getId());
                problemEmbed.setSourceType("0");
                problemEmbed.setIsActive(paragraph.getIsActive());
              //  paragraphEmbed.setSearchVector(new TSVector());
                problemEmbed.setContent(problem.getContent());
                Response<Embedding> res1 = embeddingModel.embed(problem.getContent());
                problemEmbed.setEmbedding(res1.content().vectorAsList());
                embeddingEntities.add(problemEmbed);
            }
          //  textSegmentService.saveBatch(embeddingEntities);
            embeddingMapper.insert(embeddingEntities);
            this.updateStatusById(paragraph.getId(),1,2);
            log.info("结束---->向量化段落:{}", paragraph.getId());
        }
    }


    @Transactional
    public void updateParagraphById(ParagraphEntity paragraph) {
        if (Objects.nonNull(paragraph.getIsActive())) {
            embeddingMapper.update(Wrappers.<EmbeddingEntity>lambdaUpdate().set(EmbeddingEntity::getIsActive, paragraph.getIsActive()).eq(EmbeddingEntity::getParagraphId, paragraph.getId()));
        }
        this.updateById(paragraph);
    }

    @Transactional
    public boolean deleteBatchParagraphByIds(List<String> paragraphIds) {
        if (CollectionUtils.isEmpty(paragraphIds)) {
            return false;
        }
        embeddingMapper.delete(Wrappers.<EmbeddingEntity>lambdaQuery().in(EmbeddingEntity::getParagraphId, paragraphIds));
        return this.removeByIds(paragraphIds);
    }
}
