package com.tarzan.maxkb4j.module.dataset.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    private final DataIndexService dataIndexService;

    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusById(id,type,status,type-1,type+1);
    }
    public void updateStatusByDocId(String docId, int type,int status)  {
        baseMapper.updateStatusByDocId(docId,type,status,type-1,type+1);
    }


/*    public void updateStatusByIds(List<String> paragraphIds, int type,int status)  {
        baseMapper.updateStatusByIds(paragraphIds,type,status,type-1,type+1);
    }*/

    @Transactional
    public void migrateDoc(String sourceId, String targetId, List<String> docIds) {
        //todo 优化考虑 问题同时关联了需要迁移的文档和没有迁移的文档段落时该如何处理
        dataIndexService.migrateDoc(targetId,docIds);
        this.lambdaUpdate().set(ParagraphEntity::getDatasetId, targetId).in(ParagraphEntity::getDocumentId, docIds).update();
        problemService.lambdaUpdate().set(ProblemEntity::getDatasetId, targetId).eq(ProblemEntity::getDatasetId, sourceId).update();
        problemParagraphService.lambdaUpdate().set(ProblemParagraphEntity::getDatasetId, targetId).eq(ProblemParagraphEntity::getDatasetId, sourceId).update();
    }

    @Transactional
    public void deleteByDocIds(List<String> docIds) {
        dataIndexService.removeByDocIds(docIds);
        this.lambdaUpdate().in(ParagraphEntity::getDocumentId, docIds).remove();
        //  List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery().select(ProblemParagraphEntity::getProblemId).in(ProblemParagraphEntity::getDocumentId, docIds).list();
        /*if (!CollectionUtils.isEmpty(list)) {
              // todo 如果问题关联多个段落，根据段落删除问题不太好
              problemService.removeByIds(list.stream().map(ProblemParagraphEntity::getProblemId).toList());
        }*/
        problemParagraphService.lambdaUpdate().in(ProblemParagraphEntity::getDocumentId, docIds).remove();

    }


    public void paragraphIndex(ParagraphEntity paragraph,EmbeddingModel embeddingModel) {
        if (paragraph != null) {
            this.updateStatusById(paragraph.getId(),1,1);
            //清除之前向量
            dataIndexService.removeByParagraphId(paragraph.getId());
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
            dataIndexService.insertAll(embeddingEntities);
            this.updateStatusById(paragraph.getId(),1,2);
            log.info("结束---->向量化段落:{}", paragraph.getId());
        }
    }

    @Transactional
    public void updateParagraphById(ParagraphEntity paragraph) {
        dataIndexService.updateActiveByParagraph(paragraph);
        this.updateById(paragraph);
    }

    @Transactional
    public boolean deleteBatchParagraphByIds(List<String> paragraphIds) {
        dataIndexService.removeByParagraphIds(paragraphIds);
        return this.removeByIds(paragraphIds);
    }
}
