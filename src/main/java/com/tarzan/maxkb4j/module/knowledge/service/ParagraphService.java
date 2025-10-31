package com.tarzan.maxkb4j.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.knowledge.consts.SourceType;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphAddDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.*;
import com.tarzan.maxkb4j.module.knowledge.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ModelFactory modelFactory;
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
    public void migrateDoc(String sourceKnowledgeId, String targetKnowledgeId, List<String> docIds) {
        dataIndexService.migrateDoc(targetKnowledgeId,docIds);
        this.lambdaUpdate().set(ParagraphEntity::getKnowledgeId, targetKnowledgeId).in(ParagraphEntity::getDocumentId, docIds).update();
        problemParagraphService.lambdaUpdate().eq(ProblemParagraphEntity::getKnowledgeId, sourceKnowledgeId).in(ProblemParagraphEntity::getDocumentId, docIds).remove();
    }

    @Transactional
    public void deleteByDocIds(String knowledgeId,List<String> docIds) {
        dataIndexService.removeByDocIds(knowledgeId,docIds);
        this.lambdaUpdate().in(ParagraphEntity::getDocumentId, docIds).remove();
        problemParagraphService.lambdaUpdate().in(ProblemParagraphEntity::getDocumentId, docIds).remove();

    }


    public void createIndex(ParagraphEntity paragraph, EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        if (paragraph != null) {
            this.updateStatusById(paragraph.getId(),1,1);
            //清除之前向量
            dataIndexService.removeByParagraphId(paragraph.getKnowledgeId(),paragraph.getId());
            List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
            log.info("开始---->向量化段落:{}", paragraph.getId());
            EmbeddingEntity paragraphEmbed = new EmbeddingEntity();
            paragraphEmbed.setKnowledgeId(paragraph.getKnowledgeId());
            paragraphEmbed.setDocumentId(paragraph.getDocumentId());
            paragraphEmbed.setParagraphId(paragraph.getId());
            paragraphEmbed.setSourceId(paragraph.getId());
            paragraphEmbed.setSourceType(SourceType.PARAGRAPH);
            paragraphEmbed.setIsActive(paragraph.getIsActive());
            paragraphEmbed.setContent(paragraph.getTitle() + paragraph.getContent());
            embeddingEntities.add(paragraphEmbed);
            List<ProblemEntity> problems=problemParagraphService.getProblemsByParagraphId(paragraph.getId());
            for (ProblemEntity problem : problems) {
                EmbeddingEntity problemEmbed = new EmbeddingEntity();
                problemEmbed.setKnowledgeId(paragraph.getKnowledgeId());
                problemEmbed.setDocumentId(paragraph.getDocumentId());
                problemEmbed.setParagraphId(paragraph.getId());
                problemEmbed.setSourceId(problem.getId());
                paragraphEmbed.setSourceId(problem.getId());
                problemEmbed.setSourceType(SourceType.PROBLEM);
                problemEmbed.setIsActive(paragraph.getIsActive());
                problemEmbed.setContent(problem.getContent());
                embeddingEntities.add(problemEmbed);
            }
            dataIndexService.insertAll(embeddingEntities,embeddingModel,embeddingStore);
            this.updateStatusById(paragraph.getId(),1,2);
            log.info("结束---->向量化段落:{}", paragraph.getId());
        }
    }


    @Transactional
    public void updateParagraphById(String knowledgeId,String docId,ParagraphEntity paragraph) {
        dataIndexService.updateActiveByParagraphId(knowledgeId,paragraph);
        this.updateById(paragraph);
        documentMapper.updateCharLengthById(docId);
    }


    @Transactional
    public Boolean deleteBatchByIds(String knowledgeId,String docId, List<String> paragraphIds) {
        dataIndexService.removeByParagraphIds(knowledgeId,paragraphIds);
        this.removeByIds(paragraphIds);
        return documentMapper.updateCharLengthById(docId);
    }


    @Transactional
    public boolean saveParagraphAndProblem(String knowledgeId, String docId, ParagraphAddDTO addDTO) {
        ParagraphEntity paragraph= createParagraph(knowledgeId, docId, addDTO.getTitle(), addDTO.getContent(),addDTO.getPosition());
        this.save(paragraph);
        List<ProblemEntity> problems = addDTO.getProblemList();
        //todo 问题的自动向量化
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
        return documentMapper.updateCharLengthById(docId);
    }


    public ParagraphEntity createParagraph(String knowledgeId, String docId, String title, String content,Integer  position) {
        ParagraphEntity paragraph = new ParagraphEntity();
        paragraph.setId(IdWorker.get32UUID());
        paragraph.setTitle(title == null ? "" : title);
        paragraph.setContent(content == null ? "" : content);
        paragraph.setKnowledgeId(knowledgeId);
        paragraph.setStatus("nn0");
        paragraph.setHitNum(0);
        paragraph.setIsActive(true);
        paragraph.setPosition(position==null?1:position);
        // paragraph.setStatusMeta(paragraph.defaultStatusMeta());
        paragraph.setDocumentId(docId);
        return paragraph;
    }


    public boolean save(ParagraphEntity paragraph) {
        List<ParagraphEntity> list = this.lambdaQuery().eq(ParagraphEntity::getKnowledgeId, paragraph.getKnowledgeId()).eq(ParagraphEntity::getDocumentId, paragraph.getDocumentId()).list();
        List<ParagraphEntity> updateList=list.stream().filter(e->e.getPosition()>=paragraph.getPosition()).peek(e-> e.setPosition(e.getPosition()+1)).toList();
        if (!CollectionUtils.isEmpty(updateList)){
             super.updateBatchById(updateList);
        }
        return super.save(paragraph);
    }


    @Transactional
    public boolean saveBatch(List<ParagraphEntity> paragraphs) {
        Map<String,List<ParagraphEntity>> knowledgeGroup = paragraphs.stream().collect(Collectors.groupingBy(ParagraphEntity::getKnowledgeId));
        knowledgeGroup.forEach((knowledgeId,knowledgeParagraphs)->{
            Map<String,List<ParagraphEntity>> docGroup = knowledgeParagraphs.stream().collect(Collectors.groupingBy(ParagraphEntity::getDocumentId));
            docGroup.forEach((docId,docParagraphs)->{
                long count = this.lambdaQuery().eq(ParagraphEntity::getKnowledgeId, knowledgeId).eq(ParagraphEntity::getDocumentId, docId).count();
                int position= (int) (count+1);
                for (ParagraphEntity paragraph : docParagraphs) {
                    paragraph.setPosition(position);
                    position++;
                }
            });
        });
        return super.saveBatch(paragraphs);
    }


    public IPage<ParagraphEntity> pageParagraphByDocId(String docId, int current, int size, String title, String content) {
        Page<ParagraphEntity> paragraphPage = new Page<>(current, size);
        LambdaQueryWrapper<ParagraphEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ParagraphEntity::getDocumentId, docId);
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(ParagraphEntity::getTitle, title);
        }
        if (StringUtils.isNotBlank(content)) {
            wrapper.like(ParagraphEntity::getContent, content);
        }
        wrapper.orderByAsc(ParagraphEntity::getPosition);
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
        //todo 批量生成关联 是否要修改文档状态，修改得话，定时任务会修改文档下所有段落
        this.updateStatusByDocIds(List.of(docId), 2, 0);
        documentMapper.updateStatusMetaByIds(List.of(docId));
        documentMapper.updateStatusByIds(List.of(docId), 2, 0);
        KnowledgeEntity dataset = datasetMapper.selectById(knowledgeId);
        BaseChatModel chatModel = modelFactory.build(dto.getModelId());
        EmbeddingModel embeddingModel = modelFactory.build(dataset.getEmbeddingModelId());
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

    @Transactional
    public Boolean paragraphMigrate(String sourceKnowledgeId, String sourceDocId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds) {
        dataIndexService.migrateParagraph(sourceKnowledgeId,targetKnowledgeId,targetDocId,paragraphIds);
        if (sourceKnowledgeId.equals(targetKnowledgeId)){
            problemParagraphService.lambdaUpdate()
                    .in(ProblemParagraphEntity::getParagraphId, paragraphIds)
                    .set(ProblemParagraphEntity::getKnowledgeId, targetKnowledgeId)
                    .set(ProblemParagraphEntity::getDocumentId, targetDocId)
                    .update();
        }else {
            problemParagraphService.lambdaUpdate()
                    .in(ProblemParagraphEntity::getParagraphId, paragraphIds)
                    .eq(ProblemParagraphEntity::getKnowledgeId, sourceKnowledgeId)
                    .eq(ProblemParagraphEntity::getDocumentId, sourceDocId)
                    .remove();
        }
        List<ParagraphEntity> sourceParagraphs=this.lambdaQuery().eq(ParagraphEntity::getKnowledgeId, sourceKnowledgeId).eq(ParagraphEntity::getDocumentId, sourceDocId).orderByAsc(ParagraphEntity::getPosition).list();
        int position=1;
        for (ParagraphEntity sourceParagraph : sourceParagraphs) {
            sourceParagraph.setPosition(position);
            position++;
        }
        this.updateBatchById(sourceParagraphs);
        long targetCount=this.lambdaQuery().eq(ParagraphEntity::getKnowledgeId, targetKnowledgeId).eq(ParagraphEntity::getDocumentId, targetDocId).count();
        for (String paragraphId : paragraphIds) {
            this.lambdaUpdate()
                    .set(ParagraphEntity::getKnowledgeId, targetKnowledgeId)
                    .set(ParagraphEntity::getDocumentId, targetDocId)
                    .set(ParagraphEntity::getPosition, targetCount+1)
                    .eq(ParagraphEntity::getId, paragraphId)
                    .update();
            targetCount++;
        }
        documentMapper.updateCharLengthById(sourceDocId);
        return documentMapper.updateCharLengthById(targetDocId);
    }

    @Transactional
    public boolean adjustPosition(String knowledgeId, String documentId, String paragraphId, Integer newPosition) {
        ParagraphEntity paragraph = this.getById(paragraphId);
        int oldPosition = paragraph.getPosition();
        this.lambdaUpdate().set(ParagraphEntity::getPosition, oldPosition).eq(ParagraphEntity::getKnowledgeId, knowledgeId).eq(ParagraphEntity::getDocumentId, documentId).eq(ParagraphEntity::getPosition, newPosition).update();
        paragraph.setPosition(newPosition);
        return this.updateById(paragraph);
    }
}
