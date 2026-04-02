package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.core.event.GenerateProblemEvent;
import com.maxkb4j.core.event.ParagraphIndexEvent;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.dto.ParagraphAddDTO;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.mapper.DocumentMapper;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.store.IDataStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-27 11:13:27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParagraphService extends ServiceImpl<ParagraphMapper, ParagraphEntity> implements IParagraphService{

    private final ProblemService problemService;
    private final ProblemParagraphService problemParagraphService;
    private final IDataStore compositeStore;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusByIds(List.of(id),type,status,type-1,type+1);
    }

    public void updateStatusByIds(List<String> paragraphIds, int type, int status) {
        baseMapper.updateStatusByIds(paragraphIds,type,status,type-1,type+1);
    }


    public void createIndex(ParagraphEntity paragraph, EmbeddingModel embeddingModel) {
        if (paragraph != null) {
            //清除之前向量
            compositeStore.deleteByParagraphId(paragraph.getKnowledgeId(),paragraph.getId());
            List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
            log.info("开始---->向量化段落:{}", paragraph.getId());
            String title = paragraph.getTitle() != null ? paragraph.getTitle() : "";
            String content = paragraph.getContent() != null ? paragraph.getContent() : "";
            EmbeddingEntity paragraphEmbed = EmbeddingEntity.builder()
                    .knowledgeId(paragraph.getKnowledgeId())
                    .documentId(paragraph.getDocumentId())
                    .paragraphId(paragraph.getId())
                    .sourceId(paragraph.getId())
                    .sourceType(SourceType.PARAGRAPH)
                    .content(title + content)
                    .isActive(paragraph.getIsActive())
                    .build();
            embeddingEntities.add(paragraphEmbed);
            List<ProblemEntity> problems=problemParagraphService.getProblemsByParagraphId(paragraph.getId());
            for (ProblemEntity problem : problems) {
                EmbeddingEntity problemEmbed = EmbeddingEntity.builder()
                        .knowledgeId(paragraph.getKnowledgeId())
                        .documentId(paragraph.getDocumentId())
                        .paragraphId(paragraph.getId())
                        .sourceId(problem.getId())
                        .sourceType(SourceType.PROBLEM)
                        .content(content)
                        .isActive(paragraph.getIsActive())
                        .build();
                embeddingEntities.add(problemEmbed);
            }
            compositeStore.upsert(embeddingModel,embeddingEntities);
            log.info("结束---->向量化段落:{}", paragraph.getId());
        }
    }

    /**
     * Batch create index for multiple paragraphs with optimized processing
     */
    @Override
    public void createIndexBatch(List<ParagraphEntity> paragraphs, EmbeddingModel embeddingModel) {
        if (CollectionUtils.isEmpty(paragraphs)) {
            return;
        }

        log.info("开始批量索引 {} 个段落", paragraphs.size());

        // Collect all embedding entities for batch processing
        List<EmbeddingEntity> allEmbeddingEntities = new ArrayList<>();
        List<String> paragraphIds = new ArrayList<>();

        for (ParagraphEntity paragraph : paragraphs) {
            if (paragraph == null) continue;

            // Clear previous vectors
            compositeStore.deleteByParagraphId(paragraph.getKnowledgeId(), paragraph.getId());
            paragraphIds.add(paragraph.getId());

            String title = paragraph.getTitle() != null ? paragraph.getTitle() : "";
            String content = paragraph.getContent() != null ? paragraph.getContent() : "";

            // Create paragraph embedding
            EmbeddingEntity paragraphEmbed = EmbeddingEntity.builder()
                    .knowledgeId(paragraph.getKnowledgeId())
                    .documentId(paragraph.getDocumentId())
                    .paragraphId(paragraph.getId())
                    .sourceId(paragraph.getId())
                    .sourceType(SourceType.PARAGRAPH)
                    .content(title + content)
                    .isActive(paragraph.getIsActive())
                    .build();
            allEmbeddingEntities.add(paragraphEmbed);

            // Create problem embeddings
            List<ProblemEntity> problems = problemParagraphService.getProblemsByParagraphId(paragraph.getId());
            for (ProblemEntity problem : problems) {
                EmbeddingEntity problemEmbed = EmbeddingEntity.builder()
                        .knowledgeId(paragraph.getKnowledgeId())
                        .documentId(paragraph.getDocumentId())
                        .paragraphId(paragraph.getId())
                        .sourceId(problem.getId())
                        .sourceType(SourceType.PROBLEM)
                        .content(content)
                        .isActive(paragraph.getIsActive())
                        .build();
                allEmbeddingEntities.add(problemEmbed);
            }
        }

        // Batch insert all embeddings
        if (!allEmbeddingEntities.isEmpty()) {
            compositeStore.upsert(embeddingModel, allEmbeddingEntities);
            log.info("批量索引完成，共处理 {} 个嵌入实体", allEmbeddingEntities.size());
        }
    }



    @Transactional
    public void updateParagraphById(String knowledgeId,String docId,ParagraphEntity paragraph) {
        this.updateById(paragraph);
        if (Objects.nonNull(paragraph.getContent())){
            documentMapper.updateCharLengthById(docId);
            eventPublisher.publishEvent(new ParagraphIndexEvent(this, knowledgeId,docId,List.of(paragraph.getId())));
        }
        compositeStore.updateActiveStatus(knowledgeId,paragraph.getId(),paragraph.getIsActive());
    }


    @Transactional
    public Boolean deleteBatchByIds(String knowledgeId,String docId, List<String> paragraphIds) {
        compositeStore.deleteByParagraphIds(knowledgeId,paragraphIds);
        this.removeByIds(paragraphIds);
        return documentMapper.updateCharLengthById(docId);
    }


    @Transactional
    public boolean saveParagraphAndProblem(String knowledgeId, String docId, ParagraphAddDTO addDTO) {
        ParagraphEntity paragraph= createParagraph(knowledgeId, docId, addDTO.getTitle(), addDTO.getContent(),addDTO.getPosition());
        List<ProblemEntity> problemList = addDTO.getProblemList();
        List<String> problems = new ArrayList<>();
        if (!CollectionUtils.isEmpty(problemList)) {
            problems =problemList.stream().map(ProblemEntity::getContent).toList();
        }
        return saveParagraphAndProblem(paragraph, problems);
    }



    public boolean saveParagraphAndProblem(ParagraphEntity paragraph, List<String> problems) {
        this.save(paragraph);
        if (!CollectionUtils.isEmpty(problems)) {
            List<ProblemParagraphEntity> problemParagraphMappingEntities = new ArrayList<>();
            for (String problem : problems) {
                ProblemEntity problemEntity = problemService.lambdaQuery().eq(ProblemEntity::getContent, problem).one();
                if (problemEntity == null) {
                    problemEntity = ProblemEntity.createDefault();
                    problemEntity.setHitNum(0);
                    problemEntity.setContent(problem);
                    problemEntity.setKnowledgeId(paragraph.getKnowledgeId());
                    problemService.save(problemEntity);
                }
                ProblemParagraphEntity entity = new ProblemParagraphEntity();
                entity.setKnowledgeId(paragraph.getKnowledgeId());
                entity.setProblemId(problemEntity.getId());
                entity.setParagraphId(paragraph.getId());
                entity.setDocumentId(paragraph.getDocumentId());
                problemParagraphMappingEntities.add(entity);
            }
            problemParagraphService.saveBatch(problemParagraphMappingEntities);
        }
        eventPublisher.publishEvent(new ParagraphIndexEvent(this, paragraph.getKnowledgeId(),paragraph.getDocumentId(),List.of(paragraph.getId())));
        return documentMapper.updateCharLengthById(paragraph.getDocumentId());
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
                    if (paragraph.getTitle()!=null&&paragraph.getTitle().trim().length()>256){
                        paragraph.setTitle(paragraph.getTitle().substring(0,256));
                    }
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
        this.updateStatusByIds(dto.getParagraphIdList(), 2, 0);
        eventPublisher.publishEvent(new GenerateProblemEvent(this, knowledgeId,List.of(docId),dto.getModelId(),dto.getPrompt(),List.of("0")));
        return true;
    }

    @Transactional
    public Boolean paragraphMigrate(String sourceKnowledgeId, String sourceDocId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds) {
        compositeStore.deleteByParagraphIds(sourceKnowledgeId,paragraphIds);
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
        eventPublisher.publishEvent(new ParagraphIndexEvent(this, targetKnowledgeId,targetDocId,paragraphIds));
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

    //type 1 向量化 2 问题生成 3 网络同步
    public List<ParagraphEntity> listByStateIds(String docId,int type, List<String> stateList) {
        if (type==1){
            return baseMapper.listByStateIds(docId, 3,stateList);
        }else if (type==2){
            return baseMapper.listByStateIds(docId, 2,stateList);
        }else {
            return baseMapper.listByStateIds(docId, 1,stateList);
        }

    }

    @Transactional
    public boolean deleteById(String knowledgeId,String paragraphId) {
        compositeStore.deleteByParagraphId(knowledgeId, paragraphId);
        problemParagraphService.lambdaUpdate().eq(ProblemParagraphEntity::getParagraphId, paragraphId).remove();
        return this.removeById(paragraphId);
    }
}
