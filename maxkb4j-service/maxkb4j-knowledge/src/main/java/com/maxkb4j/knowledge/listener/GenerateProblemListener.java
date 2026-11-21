package com.maxkb4j.knowledge.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.consts.TaskStatus;
import com.maxkb4j.knowledge.consts.TaskType;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.event.GenerateProblemEvent;
import com.maxkb4j.knowledge.service.IProblemService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateProblemListener {

    /**
     * 生成问题数量的缺省值，与前端表单默认值一致
     */
    private static final int DEFAULT_PROBLEM_NUMBER = 5;

    private final IDocumentInternalService documentService;
    private final IParagraphInternalService paragraphService;
    private final IModelProviderService modelFactory;
    private final IProblemService problemService;
    private final KnowledgeModelService knowledgeModelService;

    @Async
    @EventListener
    public void handleEvent(GenerateProblemEvent event) {
        log.info("收到问题生成事件消息: {}", event.getDocumentIdList());
        ChatModel chatModel = modelFactory.buildChatModel(event.getModelId(), event.getModelParamsSetting());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocumentIdList(), TaskType.GENERATE_PROBLEM, TaskStatus.PENDING);
        List<ProblemEntity> knowledgeProblems = problemService.lambdaQuery().eq(ProblemEntity::getKnowledgeId, event.getKnowledgeId()).list();
        // number 为 Integer，接口层已 @NotNull 校验；此处兜底防止内部发布方传 null 时在下方拆箱 NPE，导致全文档段落被置 FAILURE
        int problemNumber = event.getNumber() != null ? event.getNumber() : DEFAULT_PROBLEM_NUMBER;
        for (String docId : event.getDocumentIdList()) {
            try {
                List<String> paragraphIds = paragraphService.listParagraphIdsByStates(docId, TaskType.GENERATE_PROBLEM, event.getStateList());
                if (CollectionUtils.isNotEmpty(paragraphIds)) {
                    log.info("开始--->文档问题生成:{}", docId);
                    //重置完成分段数量
                    paragraphService.updateStatusByIds(paragraphIds, TaskType.GENERATE_PROBLEM, TaskStatus.STARTED);
                    documentService.updateStatusById(docId, TaskType.GENERATE_PROBLEM, TaskStatus.STARTED);
                    List<ParagraphEntity> paragraphs = paragraphService.listByIds(paragraphIds);
                    AtomicInteger failureCount = new AtomicInteger(0);
                    paragraphs.forEach(paragraph -> {
                        try {
                            problemService.generateRelated(chatModel, embeddingModel, event.getKnowledgeId(), docId, paragraph, knowledgeProblems, problemNumber);
                            paragraphService.updateStatusById(paragraph.getId(), TaskType.GENERATE_PROBLEM, TaskStatus.SUCCESS);
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            log.error("段落问题生成失败: paragraphId={}, 错误: {}", paragraph.getId(), e.getMessage(), e);
                            paragraphService.updateStatusById(paragraph.getId(), TaskType.GENERATE_PROBLEM, TaskStatus.FAILURE);
                        }
                        documentService.updateStatusMetaById(docId);
                    });
                    log.info("结束--->文档问题生成:{}", docId);
                    if (failureCount.get() > 0) {
                        log.warn("文档问题生成存在失败分段: docId={}, 失败数量: {}/{}", docId, failureCount.get(), paragraphs.size());
                        documentService.updateStatusById(docId, TaskType.GENERATE_PROBLEM, TaskStatus.FAILURE);
                        continue;
                    }
                }
                documentService.updateStatusById(docId, TaskType.GENERATE_PROBLEM, TaskStatus.SUCCESS);
            } catch (Exception e) {
                log.error("文档问题生成失败: docId={}, 错误: {}", docId, e.getMessage(), e);
                documentService.updateStatusById(docId, TaskType.GENERATE_PROBLEM, TaskStatus.FAILURE);
            }
        }
    }
}
