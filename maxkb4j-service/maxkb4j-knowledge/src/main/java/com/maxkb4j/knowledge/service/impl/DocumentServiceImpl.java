package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.dto.DatasetBatchHitHandlingDTO;
import com.maxkb4j.knowledge.dto.DocQuery;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.entity.*;
import com.maxkb4j.knowledge.event.DocumentIndexEvent;
import com.maxkb4j.knowledge.event.GenerateProblemEvent;
import com.maxkb4j.knowledge.mapper.DocumentMapper;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.service.IDocumentTagService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, DocumentEntity> implements IDocumentInternalService {

    private final ParagraphServiceImpl paragraphService;
    private final ProblemParagraphServiceImpl problemParagraphService;
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

    @Transactional
    public boolean batchHitHandling(String knowledgeId, DatasetBatchHitHandlingDTO dto) {
        List<String> ids = dto.getIdList();
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        List<DocumentEntity> documentEntities = ids.stream().map(id -> {
            DocumentEntity entity = new DocumentEntity();
            entity.setId(id);
            entity.setKnowledgeId(knowledgeId);
            entity.setHitHandlingMethod(dto.getHitHandlingMethod());
            entity.setDirectlyReturnSimilarity(dto.getDirectlyReturnSimilarity());
            return entity;
        }).collect(Collectors.toList());
        return this.updateBatchById(documentEntities);
    }

    public List<String> getNoActiveDocIds(List<String> knowledgeIds) {
        List<DocumentEntity> excludeDocuments = this.lambdaQuery()
                .select(DocumentEntity::getId)
                .in(DocumentEntity::getKnowledgeId, knowledgeIds)
                .eq(DocumentEntity::getIsActive, false)
                .list();
        return excludeDocuments.stream().map(DocumentEntity::getId).toList();
    }

    @Transactional
    public boolean deleteDocByIds(String knowledgeId, List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        problemParagraphService.lambdaUpdate().in(ProblemParagraphEntity::getDocumentId, docIds).remove();
        documentTagService.lambdaUpdate().in(DocumentTagEntity::getDocumentId, docIds).remove();
        paragraphService.lambdaUpdate().in(ParagraphEntity::getDocumentId, docIds).remove();
        compositeStore.deleteByDocumentIds(knowledgeId, docIds);
        return this.lambdaUpdate().in(DocumentEntity::getId, docIds).remove();
    }



    /**
     * 批量删除多个知识库下的文档及其标签，用 {@code IN (...)} 合并查询，避免逐个知识库往返。
     */
    @Transactional
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
        publishDocumentIndexEvent(knowledgeId, docIds, stateList);
        return true;
    }

    public boolean cancelTask(String docId, DocumentEntity doc) {
        DocumentEntity entity = baseMapper.selectById(docId);
        if (entity == null) return false;
        String status = entity.getStatus();
        if (status == null || status.length() < 3) return false;
        StringBuilder newStatus = new StringBuilder(status);
        if (doc.getType() == 1) {
            newStatus.setCharAt(2, '3'); // 向量化取消
        } else if (doc.getType() == 2) {
            newStatus.setCharAt(1, '3'); // 问题生成取消
        }
        entity.setStatus(newStatus.toString());
        return this.updateById(entity);
    }

    public DocumentEntity updateAndGetById(String docId, DocumentEntity documentEntity) {
        documentEntity.setId(docId);
        this.updateById(documentEntity);
        return this.getById(docId);
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
        eventPublisher.publishEvent(new GenerateProblemEvent(this, knowledgeId, dto.getDocumentIdList(), dto.getModelId(),dto.getModelParamsSetting(), dto.getNumber(), dto.getStateList()));
        return true;
    }

    // ===== 封装事件发布 =====
    private void publishDocumentIndexEvent(String knowledgeId, List<String> docIds, List<String> stateList) {
        if (!docIds.isEmpty()) {
            eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, stateList));
        }
    }

}
