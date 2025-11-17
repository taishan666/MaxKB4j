package com.tarzan.maxkb4j.module.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.core.assistant.ProblemGenerateAssistant;
import com.tarzan.maxkb4j.module.knowledge.consts.SourceType;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ProblemVO;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
@Slf4j
@Service
@AllArgsConstructor
public class ProblemService extends ServiceImpl<ProblemMapper, ProblemEntity> {


    private final ProblemParagraphService problemParagraphService;
    private final DataIndexService dataIndexService;


    public IPage<ProblemVO> pageByDatasetId(String id, int page, int size, String content) {
        Page<ProblemEntity> problemPage = new Page<>(page, size);
        return baseMapper.pageByDatasetId(problemPage, id, content);
    }


    public void generateRelated(ChatModel chatModel, EmbeddingModel embeddingModel, String knowledgeId, String docId, ParagraphEntity paragraph, List<ProblemEntity> knowledgeProblems, String prompt) {
        log.info("开始---->段落生成问题:{}", paragraph.getId());
        ProblemGenerateAssistant assistant = AiServices.builder(ProblemGenerateAssistant.class).chatModel(chatModel).build();
        List<String> paragraphProblems = assistant.generate(prompt.replace("{data}", paragraph.getContent()));
        List<ProblemEntity> insertProblems = new ArrayList<>();
        if (!CollectionUtils.isEmpty(paragraphProblems)) {
            for (String problem : paragraphProblems) {
                String problemId = IdWorker.get32UUID();
                ProblemEntity existingProblem = findProblem(problem, knowledgeProblems);
                if (existingProblem == null) {
                    ProblemEntity entity = ProblemEntity.createDefault();
                    entity.setId(problemId);
                    entity.setKnowledgeId(knowledgeId);
                    entity.setContent(problem);
                    insertProblems.add(entity);
                    knowledgeProblems.add(entity);
                } else {
                    problemId = existingProblem.getId();
                }
                long count = problemParagraphService.lambdaQuery().eq(ProblemParagraphEntity::getProblemId, problemId).eq(ProblemParagraphEntity::getParagraphId, paragraph.getId()).count();
                if (count == 0) {
                    ProblemParagraphEntity problemParagraph = new ProblemParagraphEntity();
                    problemParagraph.setProblemId(problemId);
                    problemParagraph.setParagraphId(paragraph.getId());
                    problemParagraph.setKnowledgeId(knowledgeId);
                    problemParagraph.setDocumentId(docId);
                    problemParagraphService.save(problemParagraph);
                }
            }
        }
        if (!CollectionUtils.isEmpty(insertProblems)) {
            baseMapper.insert(insertProblems);
            List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
            for (ProblemEntity problem : insertProblems) {
                EmbeddingEntity embeddingEntity = new EmbeddingEntity();
                embeddingEntity.setKnowledgeId(problem.getKnowledgeId());
                embeddingEntity.setDocumentId(docId);
                embeddingEntity.setParagraphId(paragraph.getId());
                embeddingEntity.setSourceId(problem.getId());
                embeddingEntity.setSourceType(SourceType.PROBLEM);
                embeddingEntity.setIsActive(true);
                embeddingEntity.setContent(problem.getContent());
                embeddingEntities.add(embeddingEntity);
            }
            dataIndexService.insertAll(embeddingEntities, embeddingModel);
        }
        log.info("结束---->段落生成问题:{}", paragraph.getId());
    }

    @Transactional
    public boolean createProblemsByDatasetId(String id, List<String> problems) {
        if (!CollectionUtils.isEmpty(problems)) {
            List<ProblemEntity> allProblems = this.lambdaQuery().eq(ProblemEntity::getKnowledgeId, id).list();
            List<ProblemEntity> problemEntities = new ArrayList<>();
            for (String problem : problems) {
                ProblemEntity existingProblem = findProblem(problem, allProblems);
                if (existingProblem == null) {
                    ProblemEntity entity = new ProblemEntity();
                    entity.setKnowledgeId(id);
                    entity.setContent(problem);
                    entity.setHitNum(0);
                    problemEntities.add(entity);
                    allProblems.add(entity);
                }
            }
            return this.saveBatch(problemEntities);
        }
        return false;
    }

    public ProblemEntity findProblem(String problem, List<ProblemEntity> allProblems) {
        ProblemEntity existingProblem = null;
        if (!CollectionUtils.isEmpty(allProblems)) {
            existingProblem = allProblems.stream()
                    .filter(e -> e.getContent().equals(problem))
                    .findFirst()
                    .orElse(null);
        }
        return existingProblem;
    }

    @Transactional
    public boolean deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return false;
        }
        problemParagraphService.lambdaUpdate().in(ProblemParagraphEntity::getProblemId, problemIds).remove();
        dataIndexService.removeBySourceIds(knowledgeId, problemIds.stream().map(String::toString).toList());
        return this.removeByIds(problemIds);
    }

    @Transactional
    public boolean createProblemsByParagraphId(String knowledgeId, String docId, String paragraphId, ProblemDTO dto) {
        ProblemEntity problem = new ProblemEntity();
        problem.setContent(dto.getContent());
        problem.setKnowledgeId(knowledgeId);
        problem.setHitNum(0);
        return this.save(problem) && problemParagraphService.association(knowledgeId, docId, paragraphId, problem.getId());
    }


}
