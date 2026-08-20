package com.maxkb4j.knowledge.service.impl;

import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.event.ParagraphIndexEvent;
import com.maxkb4j.knowledge.mapper.DocumentMapper;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.store.IDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 段落迁移服务：负责段落跨知识库 / 跨文档迁移，
 * 自 {@link ParagraphServiceImpl} 拆分而来。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ParagraphMigrationService {

    private final IParagraphInternalService paragraphService;
    private final IProblemParagraphService problemParagraphService;
    private final IDataStore compositeStore;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
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
        List<ParagraphEntity> sourceParagraphs=paragraphService.lambdaQuery().eq(ParagraphEntity::getKnowledgeId, sourceKnowledgeId).eq(ParagraphEntity::getDocumentId, sourceDocId).orderByAsc(ParagraphEntity::getPosition).list();
        int position=1;
        for (ParagraphEntity sourceParagraph : sourceParagraphs) {
            sourceParagraph.setPosition(position);
            position++;
        }
        paragraphService.updateBatchById(sourceParagraphs);
        long targetCount=paragraphService.lambdaQuery().eq(ParagraphEntity::getKnowledgeId, targetKnowledgeId).eq(ParagraphEntity::getDocumentId, targetDocId).count();
        for (String paragraphId : paragraphIds) {
            paragraphService.lambdaUpdate()
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
}
