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
import com.tarzan.maxkb4j.module.dataset.dto.DatasetBatchHitHandlingDTO;
import com.tarzan.maxkb4j.module.dataset.dto.DocumentNameDTO;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.dto.ParagraphDTO;
import com.tarzan.maxkb4j.module.dataset.entity.*;
import com.tarzan.maxkb4j.module.dataset.mapper.DatasetMapper;
import com.tarzan.maxkb4j.module.dataset.vo.*;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import com.tarzan.maxkb4j.util.BeanUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

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
    private ProblemParagraphMappingService problemParagraphMappingService;
    @Autowired
    private EmbeddingService embeddingService;

    @Value(value = "${spring.datasource.username}")
    private String dbUsername;
    @Value(value = "${spring.datasource.password}")
    private String dbPassword;
    @Value(value = "${spring.datasource.url}")
    private String dbUrl;


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
        return documentService.list();
    }

    public boolean updateProblemById(ProblemEntity problem) {
        return problemService.updateById(problem);
    }

    public boolean deleteProblemByDatasetId(UUID problemId) {
        return problemService.removeById(problemId);
    }

    public boolean deleteProblemByDatasetIds(List<String> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return false;
        }
        List<UUID> paragraphIds = problemIds.stream().map(UUID::fromString).toList();
        return problemService.lambdaUpdate().in(ProblemEntity::getId, paragraphIds).remove();
    }

    public IPage<ParagraphEntity> pageParagraphByDocId(UUID docId, int page, int size) {
        Page<ParagraphEntity> paragraphPage = new Page<>(page, size);
        return paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).page(paragraphPage);
    }

    public List<ParagraphEntity> getParagraphByProblemId(UUID problemId) {
        List<ProblemParagraphMappingEntity> list = problemParagraphMappingService.lambdaQuery()
                .select(ProblemParagraphMappingEntity::getParagraphId).eq(ProblemParagraphMappingEntity::getProblemId, problemId).list();
        if (!CollectionUtils.isEmpty(list)) {
            List<UUID> paragraphIds = list.stream().map(ProblemParagraphMappingEntity::getParagraphId).toList();
            return paragraphService.lambdaQuery().in(ParagraphEntity::getId, paragraphIds).list();
        }
        return Collections.emptyList();
    }

    public boolean association(UUID datasetId, UUID docId, UUID paragraphId, UUID problemId) {
        ProblemParagraphMappingEntity entity = new ProblemParagraphMappingEntity();
        entity.setDatasetId(datasetId);
        entity.setProblemId(problemId);
        entity.setParagraphId(paragraphId);
        entity.setDocumentId(docId);
        return problemParagraphMappingService.save(entity);
    }

    public boolean unAssociation(UUID datasetId, UUID docId, UUID paragraphId, UUID problemId) {
        return problemParagraphMappingService.lambdaUpdate()
                .eq(ProblemParagraphMappingEntity::getParagraphId, paragraphId)
                .eq(ProblemParagraphMappingEntity::getDocumentId, docId)
                .eq(ProblemParagraphMappingEntity::getProblemId, problemId)
                .eq(ProblemParagraphMappingEntity::getDatasetId, datasetId)
                .remove();
    }

    public DocumentEntity getDocByDocId(UUID docId) {
        return documentService.getById(docId);
    }

    public boolean updateParagraphByParagraphId(ParagraphEntity paragraph) {
        return paragraphService.updateById(paragraph);
    }

    public boolean deleteParagraphByParagraphId(UUID paragraphId) {
        return paragraphService.removeById(paragraphId);
    }

    public boolean deleteBatchParagraphByParagraphIds(List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return false;
        }
        List<UUID> paragraphIds = idList.stream().map(UUID::fromString).toList();
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
            List<ProblemParagraphMappingEntity> problemParagraphMappingEntities = new ArrayList<>();
            problemIds.forEach(problemId -> {
                ProblemParagraphMappingEntity entity = new ProblemParagraphMappingEntity();
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
        List<ProblemParagraphMappingEntity> list = problemParagraphMappingService.lambdaQuery()
                .select(ProblemParagraphMappingEntity::getProblemId).eq(ProblemParagraphMappingEntity::getParagraphId, paragraphId).list();
        if (!CollectionUtils.isEmpty(list)) {
            List<UUID> problemIds = list.stream().map(ProblemParagraphMappingEntity::getProblemId).toList();
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
        if(!CollectionUtils.isEmpty(docs)){
            List<DocumentEntity> documentEntities=new ArrayList<>();
            docs.forEach(e->{
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

    public boolean batchRefresh(UUID datasetId, DatasetBatchHitHandlingDTO dto) {
        List<UUID> ids = dto.getIdList();
        if (!CollectionUtils.isEmpty(ids)) {
            EmbeddingModel embeddingModel = new QwenEmbeddingModel(null,"sk-80611638022146a2bc9a1ec9b566cc54","text-embedding-v2");
            EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                    .host(this.getDbHost())
                    .port(this.getDbPort())
                    .database(this.getDbName())
                    .user(dbUsername)
                    .password(dbPassword)
                    .table("my_embeddings")
                    .dimension(embeddingModel.dimension())
                    .build();
            List<ParagraphEntity> paragraphEntities = paragraphService.lambdaQuery().in(ParagraphEntity::getDocumentId, ids).list();
            for (ParagraphEntity paragraph : paragraphEntities) {
                TextSegment segment = TextSegment.from(paragraph.getContent());
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }
         //   ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder().build();
            return true;
        }
        return false;
    }

    private String getDbName() {
        String[] split = dbUrl.split("/");
        String s = split[3];
        String[] split1 = s.split("\\?");
        return split1[0];
    }

    private String getDbHost() {
        String[] split = dbUrl.split("/");
        String s = split[2];
        String[] split1 = s.split(":");
        return split1[0];
    }

    private int getDbPort() {
        String[] split = dbUrl.split("/");
        String s = split[2];
        String[] split1 = s.split(":");
        return Integer.parseInt(split1[1]);
    }


    public List<ParagraphVO> hitTest(UUID id, HitTestDTO dto) {
        List<HitTestVO> list=embeddingService.dataSearch(id,dto);
        List<UUID> paragraphIds=list.stream().map(HitTestVO::getParagraphId).toList();
        if(CollectionUtils.isEmpty(paragraphIds)){
            return Collections.emptyList();
        }
        Map<UUID,Double> map=list.stream().collect(Collectors.toMap(HitTestVO::getParagraphId,HitTestVO::getComprehensiveScore));
        List<ParagraphVO> paragraphs= paragraphService.retrievalParagraph(paragraphIds);
        paragraphs.forEach(e->{
            e.setSimilarity(map.get(e.getId()));
            e.setComprehensiveScore(map.get(e.getId()));
        });
        return paragraphs;
    }
}
