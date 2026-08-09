package com.maxkb4j.knowledge.service.impl;

import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.event.DocumentIndexEvent;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.service.IDocumentTagService;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.store.IDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 文档迁移服务：负责文档跨知识库迁移，
 * 自 {@link DocumentServiceImpl} 拆分而来。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class DocumentMigrationService {

    private final IDocumentInternalService documentService;
    private final IParagraphInternalService paragraphService;
    private final IProblemParagraphService problemParagraphService;
    private final IDataStore compositeStore;
    private final IDocumentTagService documentTagService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public boolean migrateDoc(String sourceKnowledgeId, String targetKnowledgeId, List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        compositeStore.deleteByDocumentIds(targetKnowledgeId, docIds);
        documentTagService.lambdaUpdate().in(DocumentTagEntity::getDocumentId, docIds).remove();
        paragraphService.lambdaUpdate().set(ParagraphEntity::getKnowledgeId, targetKnowledgeId).in(ParagraphEntity::getDocumentId, docIds).update();
        problemParagraphService.lambdaUpdate().eq(ProblemParagraphEntity::getKnowledgeId, sourceKnowledgeId).in(ProblemParagraphEntity::getDocumentId, docIds).remove();
        publishDocumentIndexEvent(targetKnowledgeId, docIds, List.of("0", "1", "2", "3", "4", "5", "n"));
        return documentService.lambdaUpdate()
                .set(DocumentEntity::getKnowledgeId, targetKnowledgeId)
                .in(DocumentEntity::getId, docIds)
                .update();
    }

    private void publishDocumentIndexEvent(String knowledgeId, List<String> docIds, List<String> stateList) {
        if (!docIds.isEmpty()) {
            eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, stateList));
        }
    }
}
