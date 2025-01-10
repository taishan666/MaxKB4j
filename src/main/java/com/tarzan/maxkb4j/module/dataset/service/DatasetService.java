package com.tarzan.maxkb4j.module.dataset.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.entity.ApplicationDatasetMappingEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationDatasetMappingMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.dto.*;
import com.tarzan.maxkb4j.module.dataset.entity.*;
import com.tarzan.maxkb4j.module.dataset.mapper.DatasetMapper;
import com.tarzan.maxkb4j.module.dataset.vo.DatasetVO;
import com.tarzan.maxkb4j.module.dataset.vo.DocumentVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemVO;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@Service
public class DatasetService extends ServiceImpl<DatasetMapper, DatasetEntity> {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ProblemService problemService;
    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private ApplicationDatasetMappingMapper applicationDatasetMappingMapper;
    @Autowired
    private ParagraphService paragraphService;
    @Autowired
    private ProblemParagraphService problemParagraphMappingService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private ModelService modelService;


    public IPage<DatasetVO> selectDatasetPage(Page<DatasetVO> datasetPage, QueryDTO query) {
        return baseMapper.selectDatasetPage(datasetPage, query);
    }

    public List<DatasetEntity> getUserId(UUID userId) {
        return this.list(Wrappers.<DatasetEntity>lambdaQuery().eq(DatasetEntity::getUserId, userId));
    }

    public IPage<DocumentVO> getDocByDatasetId(UUID id, int page, int size, QueryDTO query) {
        Page<DocumentVO> docPage = new Page<>(page, size);
        return documentService.selectDocPage(docPage, id, query);
    }

    public IPage<ProblemVO> getProblemsByDatasetId(UUID id, int page, int size) {
        Page<ProblemEntity> problemPage = new Page<>(page, size);
        return problemService.getProblemsByDatasetId(problemPage, id);
    }

    public List<ApplicationEntity> getApplicationByDatasetId(String id) {
        return applicationMapper.selectList(null);
    }

    public DatasetVO getByDatasetId(UUID id) {
        DatasetEntity entity = baseMapper.selectById(id);
        DatasetVO vo = BeanUtil.copy(entity, DatasetVO.class);
        List<ApplicationDatasetMappingEntity> apps = applicationDatasetMappingMapper.selectList(Wrappers.lambdaQuery(ApplicationDatasetMappingEntity.class)
                .select(ApplicationDatasetMappingEntity::getApplicationId)
                .eq(ApplicationDatasetMappingEntity::getDatasetId, id));
        List<UUID> appIds = apps.stream().map(ApplicationDatasetMappingEntity::getApplicationId).toList();
        vo.setApplicationidList(appIds);
        return vo;
    }

    public boolean createProblemsByDatasetId(UUID id, List<String> problems) {
        if (!CollectionUtils.isEmpty(problems)) {
            List<ProblemEntity> problemEntities = new ArrayList<>();
            for (String problem : problems) {
                ProblemEntity entity = new ProblemEntity();
                entity.setDatasetId(id);
                entity.setHitNum(0);
                entity.setContent(problem);
                problemEntities.add(entity);
            }
            return problemService.saveBatch(problemEntities);
        }
        return false;
    }

    public List<DocumentEntity> listDocByDatasetId(UUID id) {
        return documentService.lambdaQuery().eq(DocumentEntity::getDatasetId, id).list();
    }

    public boolean updateProblemById(ProblemEntity problem) {
        return problemService.updateById(problem);
    }

    @Transactional
    public boolean deleteProblemByDatasetId(UUID problemId) {
        problemParagraphMappingService.lambdaUpdate().eq(ProblemParagraphEntity::getProblemId, problemId).remove();
        return problemService.removeById(problemId);
    }

    @Transactional
    public boolean deleteProblemByDatasetIds(List<String> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return false;
        }
        problemParagraphMappingService.lambdaUpdate().in(ProblemParagraphEntity::getProblemId, problemIds.stream().map(UUID::fromString).toList()).remove();
        embeddingService.lambdaUpdate().in(EmbeddingEntity::getSourceId, problemIds).remove();
        List<UUID> paragraphIds = problemIds.stream().map(UUID::fromString).toList();
        return problemService.lambdaUpdate().in(ProblemEntity::getId, paragraphIds).remove();
    }

    public IPage<ParagraphEntity> pageParagraphByDocId(UUID docId, int page, int size) {
        Page<ParagraphEntity> paragraphPage = new Page<>(page, size);
        return paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).page(paragraphPage);
    }

    public List<ParagraphEntity> getParagraphByProblemId(UUID problemId) {
        List<ProblemParagraphEntity> list = problemParagraphMappingService.lambdaQuery()
                .select(ProblemParagraphEntity::getParagraphId).eq(ProblemParagraphEntity::getProblemId, problemId).list();
        if (!CollectionUtils.isEmpty(list)) {
            List<UUID> paragraphIds = list.stream().map(ProblemParagraphEntity::getParagraphId).toList();
            return paragraphService.lambdaQuery().in(ParagraphEntity::getId, paragraphIds).list();
        }
        return Collections.emptyList();
    }

    @Transactional
    public boolean association(UUID datasetId, UUID docId, UUID paragraphId, UUID problemId) {
        ProblemParagraphEntity entity = new ProblemParagraphEntity();
        entity.setDatasetId(datasetId);
        entity.setProblemId(problemId);
        entity.setParagraphId(paragraphId);
        entity.setDocumentId(docId);
        return problemParagraphMappingService.save(entity) && embeddingService.createProblem(datasetId, docId, paragraphId, problemId);
    }

    @Transactional
    public boolean unAssociation(UUID datasetId, UUID docId, UUID paragraphId, UUID problemId) {
        return problemParagraphMappingService.lambdaUpdate()
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .eq(ProblemParagraphEntity::getDocumentId, docId)
                .eq(ProblemParagraphEntity::getProblemId, problemId)
                .eq(ProblemParagraphEntity::getDatasetId, datasetId)
                .remove() && embeddingService.lambdaUpdate().eq(EmbeddingEntity::getSourceId, problemId.toString()).eq(EmbeddingEntity::getParagraphId, paragraphId).remove();
    }

    public DocumentEntity getDocByDocId(UUID docId) {
        return documentService.getById(docId);
    }

    @Transactional
    public boolean updateParagraphByParagraphId(ParagraphEntity paragraph) {
        if(Objects.nonNull(paragraph.getIsActive())){
            embeddingService.lambdaUpdate().set(EmbeddingEntity::getIsActive,paragraph.getIsActive()).eq(EmbeddingEntity::getParagraphId,paragraph.getId()).update();
        }
        return paragraphService.updateById(paragraph);
    }
    @Transactional
    public boolean deleteParagraphByParagraphId(UUID paragraphId) {
        embeddingService.lambdaUpdate().eq(EmbeddingEntity::getParagraphId,paragraphId).remove();
        return paragraphService.removeById(paragraphId);
    }

    @Transactional
    public boolean deleteBatchParagraphByParagraphIds(List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return false;
        }
        List<UUID> paragraphIds = idList.stream().map(UUID::fromString).toList();
        embeddingService.lambdaUpdate().in(EmbeddingEntity::getParagraphId,paragraphIds).remove();
        return paragraphService.lambdaUpdate().in(ParagraphEntity::getId, paragraphIds).remove();
    }

    @Transactional
    public boolean createParagraph(ParagraphDTO paragraph) {
        boolean flag;
        paragraph.setStatus("nn2");
        paragraph.setHitNum(0);
        paragraph.setIsActive(true);
        paragraph.setStatusMeta(new JSONObject());
        flag = paragraphService.save(paragraph);
        List<ProblemEntity> problems = paragraph.getProblemList();
        if (!CollectionUtils.isEmpty(problems)) {
            List<String> problemContents = problems.stream().map(ProblemEntity::getContent).toList();
            problems = problemService.lambdaQuery().in(ProblemEntity::getContent, problemContents).list();
            List<UUID> problemIds = problems.stream().map(ProblemEntity::getId).toList();
            List<ProblemParagraphEntity> problemParagraphMappingEntities = new ArrayList<>();
            problemIds.forEach(problemId -> {
                ProblemParagraphEntity entity = new ProblemParagraphEntity();
                entity.setDatasetId(paragraph.getDatasetId());
                entity.setProblemId(problemId);
                entity.setParagraphId(paragraph.getId());
                entity.setDocumentId(paragraph.getDocumentId());
                problemParagraphMappingEntities.add(entity);
            });
            flag = problemParagraphMappingService.saveBatch(problemParagraphMappingEntities);
        }
        return flag;
    }

    public List<ProblemEntity> getProblemsByParagraphId(UUID paragraphId) {
        List<ProblemParagraphEntity> list = problemParagraphMappingService.lambdaQuery()
                .select(ProblemParagraphEntity::getProblemId).eq(ProblemParagraphEntity::getParagraphId, paragraphId).list();
        if (!CollectionUtils.isEmpty(list)) {
            List<UUID> problemIds = list.stream().map(ProblemParagraphEntity::getProblemId).toList();
            return problemService.lambdaQuery().in(ProblemEntity::getId, problemIds).list();
        }
        return Collections.emptyList();
    }


    public boolean deleteBatchDocByDocIds(List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return false;
        }
        List<UUID> docIds = idList.stream().map(UUID::fromString).toList();
        return documentService.lambdaUpdate().in(DocumentEntity::getId, docIds).remove();
    }

    public boolean batchHitHandling(UUID datasetId, DatasetBatchHitHandlingDTO dto) {
        List<UUID> ids = dto.getIdList();
        if (!CollectionUtils.isEmpty(ids)) {
            List<DocumentEntity> documentEntities = new ArrayList<>();
            ids.forEach(id -> {
                DocumentEntity entity = new DocumentEntity();
                entity.setId(id);
                entity.setDatasetId(datasetId);
                entity.setHitHandlingMethod(dto.getHitHandlingMethod());
                entity.setDirectlyReturnSimilarity(dto.getDirectlyReturnSimilarity());
                documentEntities.add(entity);
            });
            return documentService.updateBatchById(documentEntities);
        }
        return false;
    }

    public boolean migrateDoc(UUID sourceId, UUID targetId, List<UUID> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            List<DocumentEntity> documentEntities = documentService.lambdaQuery().in(DocumentEntity::getId, docIds).list();
            documentEntities.forEach(entity -> {
                entity.setId(null);
                entity.setDatasetId(targetId);
                entity.setCreateTime(null);
                entity.setUpdateTime(null);
            });
            return documentService.saveBatch(documentEntities);
        }
        return false;
    }

    public DocumentEntity updateDocByDocId(UUID docId, DocumentEntity documentEntity) {
        documentEntity.setId(docId);
        documentService.updateById(documentEntity);
        return documentEntity;
    }

    public boolean deleteDocByDocId(UUID docId) {
        return documentService.removeById(docId);
    }

    public boolean createBatchDoc(UUID datasetId, List<DocumentNameDTO> docs) {
        if (!CollectionUtils.isEmpty(docs)) {
            List<DocumentEntity> documentEntities = new ArrayList<>();
            docs.forEach(e -> {
                DocumentEntity documentEntity = new DocumentEntity();
                documentEntity.setDatasetId(datasetId);
                documentEntity.setName(e.getName());
                documentEntity.setMeta(new JSONObject());
                documentEntity.setCharLength(0);
                documentEntity.setStatus("nn2");
                documentEntity.setStatusMeta(new JSONObject());
                documentEntity.setIsActive(true);
                documentEntity.setType("0");
                documentEntity.setHitHandlingMethod("optimization");
                documentEntity.setDirectlyReturnSimilarity(0.9);
                documentEntities.add(documentEntity);
            });
            return documentService.saveBatch(documentEntities);
        }
        return false;
    }

    @Transactional
    public boolean batchRefresh(UUID datasetId, DatasetBatchHitHandlingDTO dto) {
        return embedByDocIds(datasetId,dto.getIdList());
    }

    @Transactional
    public boolean refresh(UUID datasetId,UUID docId) {
        return embedByDocIds(datasetId,List.of(docId));
    }


    public boolean embedByDocIds(UUID datasetId,List<UUID> docIds) {
        paragraphService.updateStatusByDocIds(docIds,1,0);
        documentService.updateStatusMetaByIds(docIds);
        documentService.updateStatusByIds(docIds,1,0);
        embeddingService.embedByDocIds(datasetId,docIds);
        return true;
    }


    public List<ParagraphVO> hitTest(UUID id, HitTestDTO dto) {
        return embeddingService.paragraphSearch(id,dto);
    }

    public boolean reEmbedding(UUID datasetId) {
        return embeddingService.embedByDatasetId(datasetId);
    }

    public List<ModelEntity> getModels(String id) {
        return modelService.models("LLM");
    }

    public boolean batchGenerateRelated(UUID id, GenerateProblemDTO dto) {
        paragraphService.updateStatusByDocIds(dto.getDocument_id_list(),2,0);
        documentService.updateStatusMetaByIds(dto.getDocument_id_list());
        documentService.updateStatusByIds(dto.getDocument_id_list(),2,0);
        for (UUID docId : dto.getDocument_id_list()) {
            problemService.batchGenerateRelated(id,docId, dto);
        }
        return true;
    }

    public boolean cancelTask(UUID docId,int type) {
        DocumentEntity entity=new DocumentEntity();
        entity.setId(docId);
        entity.setStatus("nn2");
        return documentService.updateById(entity);
    }
}
