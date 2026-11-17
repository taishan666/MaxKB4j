package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.consts.TaskStatus;
import com.maxkb4j.knowledge.consts.TaskType;
import com.maxkb4j.knowledge.dto.DatasetBatchHitHandlingDTO;
import com.maxkb4j.knowledge.dto.DocQuery;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.entity.*;
import com.maxkb4j.knowledge.event.DocumentIndexEvent;
import com.maxkb4j.knowledge.event.GenerateProblemEvent;
import com.maxkb4j.knowledge.mapper.DocumentMapper;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.service.IDocumentTagService;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, DocumentEntity> implements IDocumentInternalService {

    private final IParagraphInternalService paragraphService;
    private final IProblemParagraphService problemParagraphService;
    private final ApplicationEventPublisher eventPublisher;
    private final IDataStore compositeStore;
    private final IDocumentTagService documentTagService;

    public void updateStatusMetaById(String id) {
        baseMapper.updateStatusMetaByIds(List.of(id));
    }

    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusByIds(List.of(id), type, status);
    }

    public void updateStatusByIds(List<String> ids, int type, int status) {
        baseMapper.updateStatusByIds(ids, type, status);
    }

    public List<DocumentEntity> listDocByKnowledgeId(String id) {
        return this.lambdaQuery().eq(DocumentEntity::getKnowledgeId, id).list();
    }

    public DocumentEntity getByKnowledgeIdAndId(String knowledgeId, String docId) {
        return baseMapper.selectOne(
                Wrappers.<DocumentEntity>lambdaQuery()
                        .eq(DocumentEntity::getKnowledgeId, knowledgeId)
                        .eq(DocumentEntity::getId, docId));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean batchHitHandling(String knowledgeId, DatasetBatchHitHandlingDTO dto) {
        List<String> ids = dto.getIdList();
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        return baseMapper.update(null,
                Wrappers.<DocumentEntity>lambdaUpdate()
                        .eq(DocumentEntity::getKnowledgeId, knowledgeId)
                        .in(DocumentEntity::getId, ids)
                        .set(DocumentEntity::getHitHandlingMethod, dto.getHitHandlingMethod())
                        .set(dto.getDirectlyReturnSimilarity() != null,
                                DocumentEntity::getDirectlyReturnSimilarity, dto.getDirectlyReturnSimilarity())) > 0;
    }

    public List<String> getNoActiveDocIds(List<String> knowledgeIds) {
        List<DocumentEntity> excludeDocuments = this.lambdaQuery()
                .select(DocumentEntity::getId)
                .in(DocumentEntity::getKnowledgeId, knowledgeIds)
                .eq(DocumentEntity::getIsActive, false)
                .list();
        return excludeDocuments.stream().map(DocumentEntity::getId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocByIds(String knowledgeId, List<String> docIds) {
        List<String> ownedDocIds = listOwnedDocIds(knowledgeId, docIds);
        if (ownedDocIds.isEmpty()) {
            return false;
        }
        problemParagraphService.lambdaUpdate().in(ProblemParagraphEntity::getDocumentId, ownedDocIds).remove();
        documentTagService.lambdaUpdate().in(DocumentTagEntity::getDocumentId, ownedDocIds).remove();
        paragraphService.lambdaUpdate().in(ParagraphEntity::getDocumentId, ownedDocIds).remove();
        compositeStore.deleteByDocumentIds(knowledgeId, ownedDocIds);
        return this.lambdaUpdate()
                .eq(DocumentEntity::getKnowledgeId, knowledgeId)
                .in(DocumentEntity::getId, ownedDocIds)
                .remove();
    }


    /**
     * 批量删除多个知识库下的文档及其标签，用 {@code IN (...)} 合并查询，避免逐个知识库往返。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByKnowledgeIds(List<String> knowledgeIds) {
        if (CollectionUtils.isEmpty(knowledgeIds)) {
            return;
        }
        List<String> docIds = this.lambdaQuery().select(DocumentEntity::getId).in(DocumentEntity::getKnowledgeId, knowledgeIds).list().stream().map(DocumentEntity::getId).toList();
        if (!CollectionUtils.isEmpty(docIds)) {
            documentTagService.lambdaUpdate().in(DocumentTagEntity::getDocumentId, docIds).remove();
        }
        this.lambdaUpdate().in(DocumentEntity::getKnowledgeId, knowledgeIds).remove();
    }

    public boolean embedByDocIds(String knowledgeId, List<String> docIds, List<String> stateList) {
        List<String> ownedDocIds = listOwnedDocIds(knowledgeId, docIds);
        publishDocumentIndexEvent(knowledgeId, ownedDocIds, stateList);
        return !ownedDocIds.isEmpty();
    }

    public boolean cancelTask(String knowledgeId, String docId, int taskType) {
        DocumentEntity entity = getByKnowledgeIdAndId(knowledgeId, docId);
        if (entity == null) return false;
        String status = entity.getStatus();
        if (status == null || status.length() < 3) return false;
        StringBuilder newStatus = new StringBuilder(status);
        if (taskType == TaskType.EMBEDDING) {
            newStatus.setCharAt(2, (char) TaskStatus.FAILURE); // 向量化取消
        } else if (taskType == TaskType.GENERATE_PROBLEM) {
            newStatus.setCharAt(1, (char) TaskStatus.FAILURE); // 问题生成取消
        }else if (taskType == TaskType.SYNC) {
            newStatus.setCharAt(0, (char) TaskStatus.FAILURE); // 问题生成取消
        }
        return baseMapper.update(null,
                Wrappers.<DocumentEntity>lambdaUpdate()
                        .eq(DocumentEntity::getKnowledgeId, knowledgeId)
                        .eq(DocumentEntity::getId, docId)
                        .set(DocumentEntity::getStatus, newStatus.toString())) > 0;
    }

    public DocumentEntity updateAndGetById(String knowledgeId, String docId, DocumentEntity documentEntity) {
        documentEntity.setKnowledgeId(null);
        int rows = baseMapper.update(documentEntity,
                Wrappers.<DocumentEntity>lambdaUpdate()
                        .eq(DocumentEntity::getKnowledgeId, knowledgeId)
                        .eq(DocumentEntity::getId, docId));
        return rows > 0 ? getByKnowledgeIdAndId(knowledgeId, docId) : null;
    }


    public IPage<DocumentVO> getDocByKnowledgeId(String knowledgeId, int current, int size, DocQuery query) {
        Page<DocumentVO> docPage = new Page<>(current, size);
        baseMapper.selectDocPage(docPage, knowledgeId, query);
        List<DocumentVO> records = docPage.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return docPage;
        }
        List<String> docIds = records.stream().map(DocumentVO::getId).collect(Collectors.toList());
        Map<String, List<TagEntity>> tagsByDocId = documentTagService.listTagsByDocIds(docIds);
        records.forEach(doc -> doc.setTags(tagsByDocId.getOrDefault(doc.getId(), Collections.emptyList())));
        return docPage;
    }

    public boolean batchGenerateRelated(String knowledgeId, GenerateProblemDTO dto) {
        List<String> ownedDocIds = listOwnedDocIds(knowledgeId, dto.getDocumentIdList());
        if (ownedDocIds.isEmpty()) {
            return false;
        }
        eventPublisher.publishEvent(new GenerateProblemEvent(this, knowledgeId, ownedDocIds, dto.getModelId(), dto.getModelParamsSetting(), dto.getNumber(), dto.getStateList()));
        return true;
    }

    private List<String> listOwnedDocIds(String knowledgeId, List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return List.of();
        }
        return baseMapper.selectList(
                        Wrappers.<DocumentEntity>lambdaQuery()
                                .select(DocumentEntity::getId)
                                .eq(DocumentEntity::getKnowledgeId, knowledgeId)
                                .in(DocumentEntity::getId, docIds))
                .stream()
                .map(DocumentEntity::getId)
                .toList();
    }

    // ===== 封装事件发布 =====
    private void publishDocumentIndexEvent(String knowledgeId, List<String> docIds, List<String> stateList) {
        if (!docIds.isEmpty()) {
            eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, stateList));
        }
    }

}
