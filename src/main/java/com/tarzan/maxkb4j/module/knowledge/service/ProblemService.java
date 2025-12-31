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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 问题（Problem）服务层
 *
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService extends ServiceImpl<ProblemMapper, ProblemEntity> {

    private final ProblemParagraphService problemParagraphService;
    private final DataIndexService dataIndexService;

    /**
     * 分页查询指定知识库下的问题
     */
    public IPage<ProblemVO> pageByDatasetId(String knowledgeId, int page, int size, String content) {
        Page<ProblemEntity> problemPage = new Page<>(page, size);
        return baseMapper.pageByDatasetId(problemPage, knowledgeId, content);
    }

    /**
     * 基于段落内容生成相关问题，并建立关联
     */
    @Transactional
    public void generateRelated(
            ChatModel chatModel,
            EmbeddingModel embeddingModel,
            String knowledgeId,
            String docId,
            ParagraphEntity paragraph,
            List<ProblemEntity> existingProblems,
            String promptTemplate) {
        log.info("开始为段落 [{}] 生成问题", paragraph.getId());
        if (paragraph.getContent() == null) {
            log.warn("段落内容为空，跳过生成问题");
            return;
        }
        // 构建 Prompt 并调用 AI 服务
        String prompt = promptTemplate.replace("{data}", paragraph.getContent());
        ProblemGenerateAssistant assistant = AiServices.builder(ProblemGenerateAssistant.class)
                .chatModel(chatModel)
                .build();
        List<String> generatedQuestions = assistant.generate(prompt);
        if (CollectionUtils.isEmpty(generatedQuestions)) {
            log.info("AI 未生成有效问题，段落 ID: {}", paragraph.getId());
            return;
        }
        // 去重并准备新增问题
        Map<String, ProblemEntity> problemMap = existingProblems.stream()
                .collect(Collectors.toMap(ProblemEntity::getContent, p -> p, (a, b) -> a));

        List<ProblemEntity> newProblems = new ArrayList<>();
        List<ProblemParagraphEntity> associations = new ArrayList<>();
        for (String question : generatedQuestions) {
            if (problemMap.containsKey(question)) {
                // 已存在，仅建立关联
                String problemId = problemMap.get(question).getId();
                if (!isAssociationExists(problemId, paragraph.getId())) {
                    associations.add(buildProblemParagraph(knowledgeId, docId, paragraph.getId(), problemId));
                }
            } else {
                // 新增问题
                String problemId = IdWorker.get32UUID();
                ProblemEntity newProblem = ProblemEntity.createDefault();
                newProblem.setId(problemId);
                newProblem.setKnowledgeId(knowledgeId);
                newProblem.setContent(question);
                newProblems.add(newProblem);
                problemMap.put(question, newProblem); // 避免同一段落内重复
                associations.add(buildProblemParagraph(knowledgeId, docId, paragraph.getId(), problemId));
            }
        }
        // 批量保存新问题
        if (!newProblems.isEmpty()) {
            boolean saved = this.saveBatch(newProblems);
            if (!saved) {
                log.error("批量保存问题失败，段落 ID: {}", paragraph.getId());
                return;
            }
            // 构建 Embedding 实体
            List<EmbeddingEntity> embeddings = newProblems.stream()
                    .map(problem -> EmbeddingEntity.builder()
                            .knowledgeId(knowledgeId)
                            .documentId(docId)
                            .paragraphId(paragraph.getId())
                            .sourceId(problem.getId())
                            .sourceType(SourceType.PROBLEM)
                            .isActive(true)
                            .content(problem.getContent())
                            .build())
                    .collect(Collectors.toList());

            dataIndexService.insertAll(embeddings, embeddingModel);
        }
        // 批量保存关联关系（避免逐条插入）
        if (!associations.isEmpty()) {
            problemParagraphService.saveBatch(associations);
        }
        log.info("完成段落 [{}] 的问题生成，新增 {} 个问题，建立 {} 个关联", paragraph.getId(), newProblems.size(), associations.size());
    }

    /**
     * 判断问题-段落关联是否已存在（避免重复）
     */
    private boolean isAssociationExists(String problemId, String paragraphId) {
        return problemParagraphService.lambdaQuery()
                .eq(ProblemParagraphEntity::getProblemId, problemId)
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .exists();
    }

    /**
     * 构建 ProblemParagraphEntity
     */
    private ProblemParagraphEntity buildProblemParagraph(String knowledgeId, String docId, String paragraphId, String problemId) {
        ProblemParagraphEntity entity = new ProblemParagraphEntity();
        entity.setKnowledgeId(knowledgeId);
        entity.setDocumentId(docId);
        entity.setParagraphId(paragraphId);
        entity.setProblemId(problemId);
        return entity;
    }

    /**
     * 根据知识库 ID 批量创建问题（去重）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createProblemsByDatasetId(String knowledgeId, List<String> problems) {
        if (CollectionUtils.isEmpty(problems)) {
            return false;
        }

        // 获取已有问题（按内容去重）
        List<ProblemEntity> existing = this.lambdaQuery()
                .eq(ProblemEntity::getKnowledgeId, knowledgeId)
                .list();

        Set<String> existingContents = existing.stream()
                .map(ProblemEntity::getContent)
                .collect(Collectors.toSet());

        List<ProblemEntity> toInsert = problems.stream()
                .filter(content -> !existingContents.contains(content))
                .map(content -> ProblemEntity.builder()
                        .knowledgeId(knowledgeId)
                        .content(content)
                        .hitNum(0)
                        .build())
                .collect(Collectors.toList());

        if (toInsert.isEmpty()) {
            return true; // 无新问题，视为成功
        }
        return this.saveBatch(toInsert);
    }

    /**
     * 根据段落 ID 创建单个问题并关联
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createProblemsByParagraphId(String knowledgeId, String docId, String paragraphId, ProblemDTO dto) {
        if (dto == null || dto.getContent() == null) {
            return false;
        }

        ProblemEntity problem = ProblemEntity.builder()
                .knowledgeId(knowledgeId)
                .content(dto.getContent())
                .hitNum(0)
                .build();

        boolean saved = this.save(problem);
        if (!saved) {
            return false;
        }

        return problemParagraphService.association(knowledgeId, docId, paragraphId, problem.getId());
    }

    /**
     * 批量删除问题及其关联数据
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return false;
        }

        // 删除关联关系
        problemParagraphService.lambdaUpdate()
                .in(ProblemParagraphEntity::getProblemId, problemIds)
                .remove();
        // 删除向量索引
        dataIndexService.removeBySourceIds(knowledgeId, problemIds);
        // 删除问题本体
        return this.removeByIds(problemIds);
    }

    /**
     * 【辅助方法】从列表中查找相同内容的问题（建议仅用于内存小集合）
     */
    public ProblemEntity findProblem(String content, List<ProblemEntity> problems) {
        if (CollectionUtils.isEmpty(problems)) {
            return null;
        }
        return problems.stream()
                .filter(p -> Objects.equals(p.getContent(), content))
                .findFirst()
                .orElse(null);
    }
}