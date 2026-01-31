package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.core.event.DocumentIndexEvent;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentSimple;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentWriteService extends ServiceImpl<DocumentMapper, DocumentEntity> {

    private final ParagraphService paragraphService;
    private final ProblemService problemService;
    private final ProblemParagraphService problemParagraphService;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public boolean batchCreateDocs(String knowledgeId,int knowledgeType, List<DocumentSimple> docs) {
        if (CollectionUtils.isEmpty(docs)) {
            return true;
        }
        List<ProblemEntity> knowledgeProblems = problemService.lambdaQuery()
                .eq(ProblemEntity::getKnowledgeId, knowledgeId)
                .list();
        List<DocumentEntity> documentEntities = new ArrayList<>();
        List<ParagraphEntity> paragraphEntities = new ArrayList<>();
        List<ProblemParagraphEntity> problemParagraphs = new ArrayList<>();
        List<ProblemEntity> problemEntities = new ArrayList<>();
        for (DocumentSimple d : docs) {
            DocumentEntity doc = new DocumentEntity(knowledgeId, d.getName(), knowledgeType);
            AtomicInteger docCharLength = new AtomicInteger();
            if (!CollectionUtils.isEmpty(d.getParagraphs())) {
                for (var p : d.getParagraphs()) {
                    String content = p.getContent() != null ? p.getContent() : "";
                    ParagraphEntity paragraph = paragraphService.createParagraph(knowledgeId, doc.getId(), p.getTitle(), content, null);
                    paragraphEntities.add(paragraph);
                    docCharLength.addAndGet(content.length());
                    if (!CollectionUtils.isEmpty(p.getProblemList())) {
                        for (String problem : p.getProblemList()) {
                            problem = problem.trim();
                            if (problem.isEmpty()) continue;
                            String problemId = IdWorker.get32UUID();
                            ProblemEntity existingProblem = problemService.findProblem(problem, knowledgeProblems);
                            if (existingProblem == null) {
                                ProblemEntity problemEntity = ProblemEntity.createDefault();
                                problemEntity.setId(problemId);
                                problemEntity.setKnowledgeId(knowledgeId);
                                problemEntity.setContent(problem);
                                problemEntities.add(problemEntity);
                                knowledgeProblems.add(problemEntity);
                            } else {
                                problemId = existingProblem.getId();
                            }
                            if (isExistProblemParagraph(paragraph.getId(), problemId, problemParagraphs)) {
                                ProblemParagraphEntity pp = new ProblemParagraphEntity();
                                pp.setKnowledgeId(knowledgeId);
                                pp.setParagraphId(paragraph.getId());
                                pp.setDocumentId(doc.getId());
                                pp.setProblemId(problemId);
                                problemParagraphs.add(pp);
                            }
                        }
                    }
                }
            }
            doc.setCharLength(docCharLength.get());
            String sourceFileId = Optional.ofNullable(d.getSourceFileId()).orElse("");
            doc.setMeta(new JSONObject(Map.of("allow_download", true, "sourceFileId", sourceFileId)));
            documentEntities.add(doc);
        }
        this.saveBatch(documentEntities);
        if (!paragraphEntities.isEmpty()) {
            paragraphService.saveBatch(paragraphEntities);
        }
        List<String> docIds = documentEntities.stream().map(DocumentEntity::getId).toList();
        if (!problemEntities.isEmpty()) {
            problemService.saveBatch(problemEntities);
        }
        if (!problemParagraphs.isEmpty()) {
            problemParagraphService.saveBatch(problemParagraphs);
        }
        publishDocumentIndexEvent(knowledgeId, docIds, List.of("0"));
        return true;
    }

    private boolean isExistProblemParagraph(String paragraphId, String problemId, List<ProblemParagraphEntity> problemParagraphs) {
        return problemParagraphs.stream().noneMatch(e -> problemId.equals(e.getProblemId()) && paragraphId.equals(e.getParagraphId()));
    }

    // ===== 封装事件发布 =====
    private void publishDocumentIndexEvent(String knowledgeId, List<String> docIds, List<String> stateList) {
        if (!docIds.isEmpty()) {
            eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, stateList));
        }
    }
}