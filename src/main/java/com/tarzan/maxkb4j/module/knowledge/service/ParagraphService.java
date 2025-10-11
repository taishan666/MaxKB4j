package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphSimpleDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.*;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
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
    private final DocumentMapper documentMapper;
    private final ModelService modelService;
    private final KnowledgeMapper datasetMapper;

    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusById(id,type,status,type-1,type+1);
    }
    public void updateStatusByDocId(String docId, int type,int status)  {
        baseMapper.updateStatusByDocId(docId,type,status,type-1,type+1);
    }

    public void updateStatusByDocIds(List<String> docIds, int type,int status)  {
        baseMapper.updateStatusByDocIds(docIds,type,status,type-1,type+1);
    }

    @Transactional
    public void migrateDoc(String sourceId, String targetId, List<String> docIds) {
        //todo 优化考虑 问题同时关联了需要迁移的文档和没有迁移的文档段落时该如何处理
        dataIndexService.migrateDoc(targetId,docIds);
        this.lambdaUpdate().set(ParagraphEntity::getKnowledgeId, targetId).in(ParagraphEntity::getDocumentId, docIds).update();
        problemService.lambdaUpdate().set(ProblemEntity::getKnowledgeId, targetId).eq(ProblemEntity::getKnowledgeId, sourceId).update();
        problemParagraphService.lambdaUpdate().set(ProblemParagraphEntity::getKnowledgeId, targetId).eq(ProblemParagraphEntity::getKnowledgeId, sourceId).update();
    }

    @Transactional
    public void deleteByDocIds(List<String> docIds) {
        dataIndexService.removeByDocIds(docIds);
        this.lambdaUpdate().in(ParagraphEntity::getDocumentId, docIds).remove();
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
            paragraphEmbed.setKnowledgeId(paragraph.getKnowledgeId());
            paragraphEmbed.setDocumentId(paragraph.getDocumentId());
            paragraphEmbed.setParagraphId(paragraph.getId());
            paragraphEmbed.setMeta(new JSONObject());
            paragraphEmbed.setSourceId(paragraph.getId());
            paragraphEmbed.setSourceType("1");
            paragraphEmbed.setIsActive(paragraph.getIsActive());
            paragraphEmbed.setContent(paragraph.getTitle() + paragraph.getContent());
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
                problemEmbed.setKnowledgeId(paragraph.getKnowledgeId());
                problemEmbed.setDocumentId(paragraph.getDocumentId());
                problemEmbed.setParagraphId(paragraph.getId());
                problemEmbed.setMeta(new JSONObject());
                problemEmbed.setSourceId(problem.getId());
                paragraphEmbed.setSourceId(problem.getId());
                problemEmbed.setSourceType("0");
                problemEmbed.setIsActive(paragraph.getIsActive());
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
    public void updateParagraphById(String docId,ParagraphEntity paragraph) {
        dataIndexService.updateActiveByParagraph(paragraph);
        this.updateById(paragraph);
        documentMapper.updateCharLengthById(docId);
    }


    @Transactional
    public Boolean deleteBatchByIds(String docId, List<String> paragraphIds) {
        dataIndexService.removeByParagraphIds(paragraphIds);
        this.removeByIds(paragraphIds);
        return documentMapper.updateCharLengthById(docId);
    }


    @Transactional
    public boolean createParagraph(String knowledgeId, String docId, ParagraphDTO paragraph) {
        paragraph.setKnowledgeId(knowledgeId);
        paragraph.setDocumentId(docId);
        paragraph.setStatus("nn0");
        paragraph.setHitNum(0);
        paragraph.setIsActive(true);
        this.save(paragraph);
        List<ProblemEntity> problems = paragraph.getProblemList();
        if (!CollectionUtils.isEmpty(problems)) {
            List<String> problemContents = problems.stream().map(ProblemEntity::getContent).toList();
            List<ProblemParagraphEntity> problemParagraphMappingEntities = new ArrayList<>();
            for (String problemContent : problemContents) {
                ProblemEntity problem = problemService.lambdaQuery().eq(ProblemEntity::getContent, problemContent).one();
                if (problem == null) {
                    problem = ProblemEntity.createDefault();
                    problem.setHitNum(0);
                    problem.setContent(problemContent);
                    problem.setKnowledgeId(knowledgeId);
                    problemService.save(problem);
                }
                ProblemParagraphEntity entity = new ProblemParagraphEntity();
                entity.setKnowledgeId(paragraph.getKnowledgeId());
                entity.setProblemId(problem.getId());
                entity.setParagraphId(paragraph.getId());
                entity.setDocumentId(paragraph.getDocumentId());
                problemParagraphMappingEntities.add(entity);
            }
            problemParagraphService.saveBatch(problemParagraphMappingEntities);
        }
        //this.updateStatusByDocId(docId, 1, 0);
       // documentMapper.updateStatusByIds(List.of(docId), 1, 0);
        //目的是为了显示进度计数
        //documentMapper.updateStatusMetaByIds(List.of(docId));
        return documentMapper.updateCharLengthById(docId);
    }

    public ParagraphEntity createParagraph(String knowledgeId, String docId, ParagraphSimpleDTO paragraph) {
        return getParagraphEntity(knowledgeId, docId, paragraph.getTitle(), paragraph.getContent());
    }

    public ParagraphEntity getParagraphEntity(String knowledgeId, String docId, String title, String content) {
        ParagraphEntity paragraph = new ParagraphEntity();
        paragraph.setId(IdWorker.get32UUID());
        paragraph.setTitle(title == null ? "" : title);
        paragraph.setContent(content == null ? "" : content);
        paragraph.setKnowledgeId(knowledgeId);
        paragraph.setStatus("nn0");
        paragraph.setHitNum(0);
        paragraph.setIsActive(true);
        // paragraph.setStatusMeta(paragraph.defaultStatusMeta());
        paragraph.setDocumentId(docId);
        return paragraph;
    }

    public IPage<ParagraphEntity> pageParagraphByDocId(String docId, int page, int size, String title, String content) {
        Page<ParagraphEntity> paragraphPage = new Page<>(page, size);
        LambdaQueryWrapper<ParagraphEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ParagraphEntity::getDocumentId, docId);
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(ParagraphEntity::getTitle, title);
        }
        if (StringUtils.isNotBlank(content)) {
            wrapper.like(ParagraphEntity::getContent, content);
        }
        return this.page(paragraphPage, wrapper);
    }

    public List<ProblemEntity> getProblemsByParagraphId(String paragraphId) {
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery()
                .select(ProblemParagraphEntity::getProblemId).eq(ProblemParagraphEntity::getParagraphId, paragraphId).list();
        if (!CollectionUtils.isEmpty(list)) {
            List<String> problemIds = list.stream().map(ProblemParagraphEntity::getProblemId).toList();
            return problemService.lambdaQuery().in(ProblemEntity::getId, problemIds).list();
        }
        return Collections.emptyList();
    }

    public Boolean batchGenerateRelated(String knowledgeId, String docId, GenerateProblemDTO dto) {
        this.updateStatusByDocIds(List.of(docId), 2, 0);
        documentMapper.updateStatusMetaByIds(List.of(docId));
        documentMapper.updateStatusByIds(List.of(docId), 2, 0);
        KnowledgeEntity dataset = datasetMapper.selectById(knowledgeId);
        BaseChatModel chatModel = modelService.getModelById(dto.getModelId());
        EmbeddingModel embeddingModel = modelService.getModelById(dataset.getEmbeddingModelId());
        List<ParagraphEntity> paragraphs = this.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
        List<ProblemEntity> allProblems = problemService.lambdaQuery().eq(ProblemEntity::getKnowledgeId, knowledgeId).list();
        documentMapper.updateStatusByIds(List.of(docId), 2, 1);
        paragraphs.parallelStream().forEach(paragraph -> {
            problemService.generateRelated(chatModel, embeddingModel, knowledgeId, docId, paragraph, allProblems, dto);
            documentMapper.updateStatusMetaByIds(List.of(paragraph.getDocumentId()));
        });
        documentMapper.updateStatusByIds(List.of(docId), 2, 2);
        return true;
    }
}
