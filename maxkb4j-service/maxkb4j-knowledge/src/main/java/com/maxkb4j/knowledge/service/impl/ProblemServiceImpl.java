package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.core.assistant.ProblemGenerateAssistant;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.dto.ProblemDTO;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.mapper.ProblemMapper;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.service.IProblemService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.ProblemParagraphVO;
import com.maxkb4j.knowledge.vo.ProblemVO;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.Result;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, ProblemEntity> implements IProblemService {

    private final IProblemParagraphService problemParagraphService;
    private final IDataStore compositeStore;
    private final KnowledgeModelService knowledgeModelService;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    private void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页查询指定知识库下的问题
     */
    public IPage<ProblemVO> pageByDatasetId(String knowledgeId, int page, int size, String content) {
        Page<ProblemEntity> problemPage = new Page<>(page, size);
        return baseMapper.pageByDatasetId(problemPage, knowledgeId, content);
    }

    /**
     * 基于段落内容生成相关问题，并建立关联
     *
     * <p>注意：本方法故意不加事务。LLM 生成、Embedding 与向量写入均为远程调用，
     * 不能放进本地事务（会长时间占用连接，且 DB 与向量库无法保证一致性）。
     * 各 DB 写入依赖 MyBatis-Plus {@code saveBatch} 自带的短事务。
     */
    public void generateRelated(
            ChatModel chatModel,
            EmbeddingModel embeddingModel,
            String knowledgeId,
            String docId,
            ParagraphEntity paragraph,
            List<ProblemEntity> existingProblems,
            int problemNumber) {
        log.info("开始为段落 [{}] 生成问题", paragraph.getId());
        if (paragraph.getContent() == null) {
            log.warn("段落内容为空，跳过生成问题");
            return;
        }
        // 构建 Prompt 并调用 AI 服务
        ProblemGenerateAssistant assistant = AiServiceFactory.builder(ProblemGenerateAssistant.class)
                .chatModel(chatModel)
                .build();
        Result<List<String>> result = assistant.generate(problemNumber, paragraph.getContent());
        List<String> generatedQuestions = result.content();
        if (CollectionUtils.isEmpty(generatedQuestions)) {
            log.info("AI 未生成有效问题，段落 ID: {}", paragraph.getId());
            return;
        }
        // LLM 可能返回重复或空问题：过滤空值并按内容去重，避免同一 problemId 的实体重复入批、关联表插入重复行
        generatedQuestions = generatedQuestions.stream().filter(q -> q != null && !q.isBlank()).distinct().toList();
        // content -> problemId：已有问题与本批新问题共用一张映射表，保证同一内容只分配一个 ID
        Map<String, String> problemMap = existingProblems.stream()
                .collect(Collectors.toMap(ProblemEntity::getContent, ProblemEntity::getId, (n, o) -> n));

        // 新问题实体在此一次性创建并按内容登记，杜绝同 ID 实体重复入批
        Map<String, ProblemEntity> newProblemMap = new LinkedHashMap<>();
        List<String> allProblemIds = new ArrayList<>();
        for (String question : generatedQuestions) {
            String problemId = problemMap.get(question);
            if (problemId == null) {
                problemId = IdWorker.get32UUID();
                problemMap.put(question, problemId);
                ProblemEntity newProblem = ProblemEntity.createDefault();
                newProblem.setId(problemId);
                newProblem.setKnowledgeId(knowledgeId);
                newProblem.setContent(question);
                newProblemMap.put(question, newProblem);
            }
            allProblemIds.add(problemId);
        }

        // 批量查询已存在的关联
        Set<String> existingAssocSet = findExistingAssociations(allProblemIds, paragraph.getId());

        // 关联表每行只构建一次：已存在的关联跳过，其余按 problemId 建行
        List<ProblemParagraphVO> associations = new ArrayList<>();
        for (String question : generatedQuestions) {
            String problemId = problemMap.get(question);
            if (!existingAssocSet.contains(problemId)) {
                associations.add(buildProblemParagraph(knowledgeId, docId, paragraph.getId(), problemId, question));
            }
        }
        List<ProblemEntity> newProblems = new ArrayList<>(newProblemMap.values());
        // 短事务：仅落库，不包含远程调用
        if (!newProblems.isEmpty()) {
            boolean saved = this.saveBatchProblems(newProblems);
            if (!saved) {
                log.error("批量保存问题失败，段落 ID: {}", paragraph.getId());
                return;
            }
        }
        // 批量保存关联关系（避免逐条插入）
        if (!associations.isEmpty()) {
            List<ProblemParagraphEntity> entities = new ArrayList<>(associations);
            problemParagraphService.saveBatch(entities);
        }
        // 事务外异步化向量化：Embedding 与外部索引写入不放进本地事务
        if (!newProblems.isEmpty()) {
            try {
                reIndexBatch(knowledgeId, newProblems.stream().map(ProblemEntity::getId).toList());
            } catch (Exception e) {
                log.error("问题向量索引失败，知识库 ID: {}，问题数: {}", knowledgeId, newProblems.size(), e);
            }
        }
        log.info("完成段落 [{}] 的问题生成，新增 {} 个问题，建立 {} 个关联", paragraph.getId(), newProblems.size(), associations.size());
    }

    /**
     * 仅做 DB 落库的短事务方法；向量索引由调用方在事务外触发。
     *
     * <p>本方法只被同类 {@code this.} 自调用（generateRelated / createProblemsByDatasetId），
     * 代理会被绕过导致 {@code @Transactional} 失效，故改用 {@link TransactionTemplate} 编程式事务。
     * 事务激活后，MyBatis-Plus {@code saveBatch} 内部的批量会话会加入当前事务，随其统一提交/回滚。
     */
    public boolean saveBatchProblems(List<ProblemEntity> problems) {
        if (problems.isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> this.saveBatch(problems)));
    }

    public void reIndexBatch(String knowledgeId, List<String> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return;
        }
        // 先校验嵌入模型再删旧向量：模型缺失或配置错误时快速失败，避免旧向量被删后无法重建
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(knowledgeId);
        if (embeddingModel == null) {
            throw new IllegalStateException("知识库未配置嵌入模型，无法重建问题向量, knowledgeId: " + knowledgeId);
        }
        compositeStore.deleteByProblemIds(knowledgeId, problemIds);
        createIndexBatch(problemIds, embeddingModel);
    }

    /**
     * Batch create index for multiple paragraphs with optimized processing
     */
    private void createIndexBatch(List<String> problemIds, EmbeddingModel embeddingModel) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return;
        }
        List<ProblemEntity> problems = this.listByIds(problemIds);
        log.info("开始批量索引 {} 个问题", problems.size());
        // Collect all embedding entities for batch processing
        List<EmbeddingEntity> allEmbeddingEntities = new ArrayList<>();
        for (ProblemEntity problem : problems) {
            if (problem == null) continue;
            // Clear previous vectors
            String content = problem.getContent() != null ? problem.getContent() : "";

            // Create paragraph embedding
            EmbeddingEntity paragraphEmbed = EmbeddingEntity.builder()
                    .knowledgeId(problem.getKnowledgeId())
                    .sourceId(problem.getId())
                    .sourceType(SourceType.PROBLEM)
                    .content(content)
                    .build();
            allEmbeddingEntities.add(paragraphEmbed);
        }

        // Batch insert all embeddings
        if (!allEmbeddingEntities.isEmpty()) {
            compositeStore.upsert(embeddingModel, allEmbeddingEntities);
            log.info("批量索引完成，共处理 {} 个嵌入实体", allEmbeddingEntities.size());
        }
    }


    /**
     * 批量判断问题-段落关联是否已存在（避免 N+1 查询）
     */
    private Set<String> findExistingAssociations(List<String> problemIds, String paragraphId) {
        if (problemIds == null || problemIds.isEmpty()) {
            return Set.of();
        }
        return problemParagraphService.lambdaQuery()
                .select(ProblemParagraphEntity::getProblemId)
                .in(ProblemParagraphEntity::getProblemId, problemIds)
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .list()
                .stream()
                .map(ProblemParagraphEntity::getProblemId)
                .collect(Collectors.toSet());
    }

    /**
     * 构建 ProblemParagraphEntity
     */
    private ProblemParagraphVO buildProblemParagraph(String knowledgeId, String docId, String paragraphId, String problemId, String content) {
        ProblemParagraphVO vo = new ProblemParagraphVO();
        vo.setKnowledgeId(knowledgeId);
        vo.setDocumentId(docId);
        vo.setParagraphId(paragraphId);
        vo.setProblemId(problemId);
        vo.setContent(content);
        return vo;
    }

    /**
     * 根据知识库 ID 批量创建问题（去重）
     *
     * <p>不带事务：落库走 {@link #saveBatchProblems} 的短事务，向量化在事务外执行
     */
    public boolean createProblemsByDatasetId(String knowledgeId, List<String> problems) {
        if (CollectionUtils.isEmpty(problems)) {
            return false;
        }
        problems = problems.stream().distinct().toList();
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
        boolean saved = saveBatchProblems(toInsert);
        if (saved && !toInsert.isEmpty()) {
            try {
                reIndexBatch(knowledgeId, toInsert.stream().map(ProblemEntity::getId).toList());
            } catch (Exception e) {
                log.error("问题向量索引失败，知识库 ID: {}", knowledgeId, e);
            }
        }
        return saved;
    }

    /**
     * 根据段落 ID 创建单个问题并关联
     *
     * <p>不带事务：DB 写入各自为短事务，Embedding 与向量写入在事务外执行
     */
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
        try {
            reIndexBatch(knowledgeId, List.of(problem.getId()));
        } catch (Exception e) {
            log.error("问题向量索引失败，知识库 ID: {}，问题 ID: {}", knowledgeId, problem.getId(), e);
        }
        return problemParagraphService.association(knowledgeId, docId, paragraphId, problem.getId());
    }

    /**
     * 批量删除问题及其关联数据
     *
     * <p>DB 删除走本地短事务；向量库删除在外部存储侧执行，不放进事务
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return false;
        }
        // 删除关联关系与问题本体（同一短事务）
        problemParagraphService.lambdaUpdate()
                .eq(ProblemParagraphEntity::getKnowledgeId, knowledgeId)
                .in(ProblemParagraphEntity::getProblemId, problemIds)
                .remove();
        boolean removed = this.lambdaUpdate()
                .eq(ProblemEntity::getKnowledgeId, knowledgeId)
                .in(ProblemEntity::getId, problemIds)
                .remove();
        // 事务提交后再清理向量索引，避免外部存储调用延长事务
        if (removed) {
            registerAfterCommit(() -> {
                try {
                    compositeStore.deleteByProblemIds(knowledgeId, problemIds);
                } catch (Exception e) {
                    log.error("删除问题向量索引失败，知识库 ID: {}，问题数: {}", knowledgeId, problemIds.size(), e);
                }
            });
        }
        return removed;
    }

    /**
     * 事务提交后执行外部存储操作；无事务上下文时立即执行
     */
    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
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

    /**
     * 更新问题并重建向量索引
     *
     * <p>DB 更新为短事务；向量删除与重建（含 Embedding 远程调用）在事务提交后执行
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProblemById(ProblemEntity problem) {
        boolean updated = this.lambdaUpdate()
                .eq(ProblemEntity::getId, problem.getId())
                .eq(ProblemEntity::getKnowledgeId, problem.getKnowledgeId())
                .update(problem);
        if (updated) {
            registerAfterCommit(() -> {
                try {
                    reIndexBatch(problem.getKnowledgeId(), List.of(problem.getId()));
                } catch (Exception e) {
                    log.error("重建问题向量索引失败，问题 ID: {}", problem.getId(), e);
                }
            });
        }
        return updated;
    }
}