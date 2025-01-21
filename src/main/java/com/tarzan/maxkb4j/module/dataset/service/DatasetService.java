package com.tarzan.maxkb4j.module.dataset.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationDatasetMappingEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationDatasetMappingMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.dataset.dto.*;
import com.tarzan.maxkb4j.module.dataset.entity.*;
import com.tarzan.maxkb4j.module.dataset.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.dataset.mapper.DatasetMapper;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.dataset.vo.*;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.BeanUtil;
import com.tarzan.maxkb4j.util.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@Slf4j
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
    @Autowired
    private ProblemParagraphService problemParagraphService;
    @Autowired
    private ProblemMapper problemMapper;


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

    public IPage<ProblemVO> getProblemsByDatasetId(UUID id, int page, int size, String content) {
        Page<ProblemEntity> problemPage = new Page<>(page, size);
        return problemService.getProblemsByDatasetId(problemPage, id, content);
    }

    public List<ApplicationEntity> getApplicationByDatasetId(UUID id) {
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
            List<ProblemEntity> dbProblemEntities = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, id).list();
            List<ProblemEntity> problemEntities = new ArrayList<>();
            for (String problem : problems) {
                ProblemEntity existingProblem = findProblem(problem, problemEntities, dbProblemEntities);
                if (existingProblem == null) {
                    ProblemEntity entity = new ProblemEntity();
                    entity.setDatasetId(id);
                    entity.setHitNum(0);
                    entity.setContent(problem);
                    problemEntities.add(entity);
                }
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
    public boolean deleteProblemByDatasetIds(List<UUID> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return false;
        }
        problemParagraphMappingService.lambdaUpdate().in(ProblemParagraphEntity::getProblemId, problemIds).remove();
        embeddingService.lambdaUpdate().in(EmbeddingEntity::getSourceId, problemIds.stream().map(UUID::toString).toList()).remove();
        return problemService.lambdaUpdate().in(ProblemEntity::getId, problemIds).remove();
    }

    public IPage<ParagraphEntity> pageParagraphByDocId(UUID docId, int page, int size, String title, String content) {
        Page<ParagraphEntity> paragraphPage = new Page<>(page, size);
        LambdaQueryWrapper<ParagraphEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ParagraphEntity::getDocumentId, docId);
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(ParagraphEntity::getTitle, title);
        }
        if (StringUtils.isNotBlank(content)) {
            wrapper.like(ParagraphEntity::getContent, content);
        }
        return paragraphService.page(paragraphPage, wrapper);
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
        if (Objects.nonNull(paragraph.getIsActive())) {
            embeddingService.lambdaUpdate().set(EmbeddingEntity::getIsActive, paragraph.getIsActive()).eq(EmbeddingEntity::getParagraphId, paragraph.getId()).update();
        }
        return paragraphService.updateById(paragraph);
    }

    @Transactional
    public boolean deleteParagraphByParagraphId(UUID paragraphId) {
        embeddingService.lambdaUpdate().eq(EmbeddingEntity::getParagraphId, paragraphId).remove();
        return paragraphService.removeById(paragraphId);
    }

    @Transactional
    public boolean deleteBatchParagraphByParagraphIds(List<UUID> paragraphIds) {
        if (CollectionUtils.isEmpty(paragraphIds)) {
            return false;
        }
        embeddingService.lambdaUpdate().in(EmbeddingEntity::getParagraphId, paragraphIds).remove();
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


    @Transactional
    public boolean deleteBatchDocByDocIds(List<UUID> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        embeddingService.lambdaUpdate().in(EmbeddingEntity::getDocumentId, docIds).remove();
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery().select(ProblemParagraphEntity::getProblemId).in(ProblemParagraphEntity::getDocumentId, docIds).list();
        problemParagraphService.lambdaUpdate().in(ProblemParagraphEntity::getDocumentId, docIds).remove();
        paragraphService.lambdaUpdate().in(ParagraphEntity::getDocumentId, docIds).remove();
        if (!CollectionUtils.isEmpty(list)) {
            problemService.lambdaUpdate().in(ProblemEntity::getId, list.stream().map(ProblemParagraphEntity::getProblemId).toList()).remove();
            //  problemService.removeByIds(list.stream().map(ProblemParagraphEntity::getProblemId).toList());
        }
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

    @Transactional
    public boolean migrateDoc(UUID sourceId, UUID targetId, List<UUID> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            embeddingService.lambdaUpdate().set(EmbeddingEntity::getDatasetId, targetId).eq(EmbeddingEntity::getDatasetId, sourceId).update();
            paragraphService.lambdaUpdate().set(ParagraphEntity::getDatasetId, targetId).eq(ParagraphEntity::getDatasetId, sourceId).update();
            problemService.lambdaUpdate().set(ProblemEntity::getDatasetId, targetId).eq(ProblemEntity::getDatasetId, sourceId).update();
            problemParagraphService.lambdaUpdate().set(ProblemParagraphEntity::getDatasetId, targetId).eq(ProblemParagraphEntity::getDatasetId, sourceId).update();
            return documentService.lambdaUpdate().set(DocumentEntity::getDatasetId, targetId).eq(DocumentEntity::getDatasetId, sourceId).update();
        }
        return false;
    }

    public boolean copyDoc(UUID sourceId, UUID targetId, List<UUID> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            List<DocumentEntity> documentEntities = documentService.lambdaQuery()
                    .eq(DocumentEntity::getDatasetId, sourceId).in(DocumentEntity::getId, docIds).list();
            List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery()
                    .eq(ParagraphEntity::getDatasetId, sourceId).in(ParagraphEntity::getDocumentId, docIds).list();
            List<ProblemParagraphEntity> problemParagraphs = problemParagraphService.lambdaQuery()
                    .eq(ProblemParagraphEntity::getDatasetId, sourceId).in(ProblemParagraphEntity::getDocumentId, docIds).list();
            List<ProblemEntity> problems;
            if (!CollectionUtils.isEmpty(problemParagraphs)) {
                problems = problemService.listByIds(problemParagraphs.stream().map(ProblemParagraphEntity::getProblemId).toList());
            } else {
                problems = new ArrayList<>();
            }
            Map<UUID, UUID> docIdMap = documentEntities.stream().collect(Collectors.toMap(DocumentEntity::getId, doc -> UUID.randomUUID()));
            Map<UUID, UUID> paragraphIdMap = paragraphs.stream().collect(Collectors.toMap(ParagraphEntity::getId, paragraph -> UUID.randomUUID()));
            Map<UUID, UUID> problemIdMap = problems.stream().collect(Collectors.toMap(ProblemEntity::getId, problem -> UUID.randomUUID()));
            documentEntities.forEach(doc -> {
                if (!CollectionUtils.isEmpty(paragraphs)) {
                    List<ParagraphEntity> docParagraphs = paragraphs.stream().filter(paragraph -> paragraph.getDocumentId().equals(doc.getId())).toList();
                    docParagraphs.forEach(paragraph -> {
                        if (!CollectionUtils.isEmpty(problemParagraphs)) {
                            List<ProblemParagraphEntity> docProblemParagraphs = problemParagraphs.stream().filter(e -> e.getParagraphId().equals(paragraph.getId())).toList();
                            docProblemParagraphs.forEach(problemParagraph -> {
                                problemParagraph.setId(null);
                                problemParagraph.setParagraphId(paragraphIdMap.get(paragraph.getId()));
                                problemParagraph.setDocumentId(docIdMap.get(doc.getId()));
                                problemParagraph.setDatasetId(targetId);
                                problemParagraph.setCreateTime(null);
                                problemParagraph.setUpdateTime(null);
                                if (!CollectionUtils.isEmpty(problems)) {
                                    ProblemEntity problem = problems.stream().filter(e -> e.getId().equals(problemParagraph.getProblemId())).findFirst().orElse(null);
                                    if (problem != null) {
                                        problem.setId(problemIdMap.get(problem.getId()));
                                        problem.setDatasetId(targetId);
                                        problem.setCreateTime(null);
                                        problem.setUpdateTime(null);
                                        problemParagraph.setProblemId(problem.getId());
                                    }
                                }

                            });
                        }
                        paragraph.setId(paragraphIdMap.get(paragraph.getId()));
                        paragraph.setDocumentId(docIdMap.get(doc.getId()));
                        paragraph.setDatasetId(targetId);
                        paragraph.setCreateTime(null);
                        paragraph.setUpdateTime(null);
                    });
                }
                doc.setId(docIdMap.get(doc.getId()));
                doc.setDatasetId(targetId);
                doc.setCreateTime(null);
                doc.setUpdateTime(null);
            });
            documentService.saveBatch(documentEntities);
            problemService.saveBatch(problems);
            paragraphService.saveBatch(paragraphs);
            return problemParagraphService.saveBatch(problemParagraphs);
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
                documentEntities.add(createDocument(datasetId, e.getName()));
            });
            return documentService.saveBatch(documentEntities);
        }
        return false;
    }

    public DocumentEntity createDocument(UUID datasetId, String name) {
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setId(UUID.randomUUID());
        documentEntity.setDatasetId(datasetId);
        documentEntity.setName(name);
        documentEntity.setMeta(new JSONObject());
        documentEntity.setCharLength(0);
        documentEntity.setStatus("nn2");
        documentEntity.setStatusMeta(new JSONObject());
        documentEntity.setIsActive(true);
        documentEntity.setType("0");
        documentEntity.setHitHandlingMethod("optimization");
        documentEntity.setDirectlyReturnSimilarity(0.9);
        return documentEntity;

    }

    @Transactional
    public boolean batchRefresh(UUID datasetId, DatasetBatchHitHandlingDTO dto) {
        return embedByDocIds(datasetId, dto.getIdList());
    }

    @Transactional
    public boolean refresh(UUID datasetId, UUID docId) {
        return embedByDocIds(datasetId, List.of(docId));
    }


    public boolean embedByDocIds(UUID datasetId, List<UUID> docIds) {
        paragraphService.updateStatusByDocIds(docIds, 1, 0);
        documentService.updateStatusMetaByIds(docIds);
        documentService.updateStatusByIds(docIds, 1, 0);
        embeddingService.embedByDocIds(datasetId, docIds);
        return true;
    }


    public List<ParagraphVO> hitTest(List<UUID> ids, HitTestDTO dto) {
        return embeddingService.paragraphSearch(ids, dto);
    }

    public List<ParagraphVO> hitTest(UUID id, HitTestDTO dto) {
        return embeddingService.paragraphSearch(List.of(id), dto);
    }

    public boolean reEmbedding(UUID datasetId) {
        return embeddingService.embedByDatasetId(datasetId);
    }

    public List<ModelEntity> getModels(UUID id) {
        return modelService.models("LLM");
    }

    public boolean batchGenerateRelated(UUID datasetId, GenerateProblemDTO dto) {
        paragraphService.updateStatusByDocIds(dto.getDocument_id_list(), 2, 0);
        documentService.updateStatusMetaByIds(dto.getDocument_id_list());
        documentService.updateStatusByIds(dto.getDocument_id_list(), 2, 0);
        for (UUID docId : dto.getDocument_id_list()) {
            List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
            problemService.batchGenerateRelated(datasetId, docId, paragraphs, dto);
        }
        return true;
    }

    public boolean cancelTask(UUID docId, int type) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(docId);
        entity.setStatus("nn2");
        return documentService.updateById(entity);
    }

    @Transactional
    public Boolean deleteDatasetById(UUID id) {
        problemParagraphMappingService.lambdaUpdate().eq(ProblemParagraphEntity::getDatasetId, id).remove();
        problemService.lambdaUpdate().eq(ProblemEntity::getDatasetId, id).remove();
        paragraphService.lambdaUpdate().eq(ParagraphEntity::getDatasetId, id).remove();
        documentService.lambdaUpdate().eq(DocumentEntity::getDatasetId, id).remove();
        applicationDatasetMappingMapper.delete(Wrappers.<ApplicationDatasetMappingEntity>lambdaQuery().eq(ApplicationDatasetMappingEntity::getDatasetId, id));
        embeddingService.lambdaUpdate().eq(EmbeddingEntity::getDatasetId, id).remove();
        return this.removeById(id);
    }


    public void exportExcelZipByDocs(List<DocumentEntity> docs, String exportName, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(exportName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".zip");
        // 创建字节输出流和ZIP输出流
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        // 使用ByteArrayOutputStream作为临时存储Excel文件
        ByteArrayOutputStream excelOutputStream = new ByteArrayOutputStream();
        ExcelWriter excelWriter = EasyExcel.write(excelOutputStream, DatasetExcel.class).build();
        for (DocumentEntity doc : docs) {
            List<DatasetExcel> list = getDatasetExcelByDoc(doc);
            // 使用同一个写入器添加新的 sheet 页
            WriteSheet writeSheet = EasyExcel.writerSheet(doc.getName()).build();
            excelWriter.write(list, writeSheet);
        }
        // 完成写入操作
        excelWriter.finish();
        // 将生成的Excel添加到ZIP中
        ZipEntry zipEntry = new ZipEntry(exportName + ".xlsx");
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(excelOutputStream.toByteArray()); // 将字节输出流转为字节数组写入
        zipOutputStream.closeEntry();

        // 关闭Excel相关的资源
        excelOutputStream.close();

        // 完成ZIP文件的写入
        zipOutputStream.finish();
        zipOutputStream.close();

        // 将所有数据写入最终的输出流
        OutputStream outputStream = response.getOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        outputStream.flush();
        byteArrayOutputStream.close(); // 关闭字节输出流
    }

    public void exportExcelZipByDatasetId(UUID id, HttpServletResponse response) throws IOException {
        DatasetEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentService.lambdaQuery().eq(DocumentEntity::getDatasetId, id).list();
        exportExcelZipByDocs(docs, dataset.getName(), response);
    }

    public void exportExcelByDatasetId(UUID id, HttpServletResponse response) throws IOException {
        DatasetEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentService.lambdaQuery().eq(DocumentEntity::getDatasetId, id).list();
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(dataset.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        OutputStream outputStream = response.getOutputStream();
        // 创建 EasyExcel 写入器
        ExcelWriter excelWriter = EasyExcel.write(outputStream, DatasetExcel.class).build();
        for (DocumentEntity doc : docs) {
            List<DatasetExcel> list = getDatasetExcelByDoc(doc);
            // 使用同一个写入器添加新的 sheet 页
            WriteSheet writeSheet = EasyExcel.writerSheet(doc.getName()).build();
            excelWriter.write(list, writeSheet);
        }
        // 完成写入操作
        excelWriter.finish();
    }

    public void exportExcelByDocId(UUID docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = documentService.getById(docId);
        exportExcelZipByDocs(List.of(doc), doc.getName(), response);
    }

    public void exportExcelZipByDocId(UUID docId, HttpServletResponse response) {
        DocumentEntity doc = documentService.getById(docId);
        List<DatasetExcel> list = getDatasetExcelByDoc(doc);
        ExcelUtil.export(response, doc.getName(), doc.getName(), list, DatasetExcel.class);
    }

    private List<DatasetExcel> getDatasetExcelByDoc(DocumentEntity doc) {
        List<DatasetExcel> list = new ArrayList<>();
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, doc.getId()).list();
        for (ParagraphEntity paragraph : paragraphs) {
            DatasetExcel excel = new DatasetExcel();
            excel.setTitle(paragraph.getTitle());
            excel.setContent(paragraph.getContent());
            List<ProblemEntity> problemEntities = problemParagraphMappingService.getProblemsByParagraphId(paragraph.getId());
            StringBuilder sb = new StringBuilder();
            if (!CollectionUtils.isEmpty(problemEntities)) {
                List<String> problems = problemEntities.stream().map(ProblemEntity::getContent).toList();
                String result = String.join("\n", problems);
                sb.append(result);
            }
            excel.setProblems(sb.toString());
            list.add(excel);
        }
        return list;
    }

    @Transactional
    protected void processExcelFile(UUID datasetId, byte[] fileBytes) throws IOException {
        try (InputStream fis = new ByteArrayInputStream(fileBytes)) {
            Workbook workbook = WorkbookFactory.create(fis);
            int numberOfSheets = workbook.getNumberOfSheets();
            System.out.println("Sheet 数量: " + numberOfSheets);
            List<ProblemEntity> dbProblemEntities = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
            List<ProblemEntity> problemEntities = new ArrayList<>();
            List<ProblemParagraphEntity> problemParagraphs = new ArrayList<>();
            List<ParagraphEntity> paragraphs = new ArrayList<>();
            List<DocumentEntity> docs = new ArrayList<>();
            for (int i = 0; i < numberOfSheets; i++) {
                String sheetName = workbook.getSheetName(i);
                System.out.println("Sheet 名称: " + sheetName);
                DocumentEntity doc = createDocument(datasetId, sheetName);
                // 对于每一个Sheet进行数据读取
                EasyExcel.read(new ByteArrayInputStream(fileBytes))
                        .sheet(sheetName) // 使用Sheet编号读取
                        .head(DatasetExcel.class)
                        .registerReadListener(new PageReadListener<DatasetExcel>(dataList -> {
                            for (DatasetExcel data : dataList) {
                                log.info("在Sheet {} 中读取到一条数据{}", sheetName, JSON.toJSONString(data));
                                ParagraphEntity paragraph = getParagraphEntity(datasetId, data.getTitle(),data.getContent(), doc.getId());
                                paragraphs.add(paragraph);
                                doc.setCharLength(doc.getCharLength() + paragraph.getContent().length());
                                if (StringUtils.isNotBlank(data.getProblems())) {
                                    String[] problems = data.getProblems().split("\n");
                                    for (String problem : problems) {
                                        UUID problemId = UUID.randomUUID();
                                        ProblemEntity existingProblem = findProblem(problem, problemEntities, dbProblemEntities);
                                        if (existingProblem == null) {
                                            ProblemEntity problemEntity = new ProblemEntity();
                                            problemEntity.setId(problemId);
                                            problemEntity.setDatasetId(datasetId);
                                            problemEntity.setContent(problem);
                                            problemEntity.setHitNum(0);
                                            problemEntities.add(problemEntity);
                                        } else {
                                            problemId = existingProblem.getId();
                                        }
                                        if (!isExistProblemParagraph(paragraph.getId(), problemId, problemParagraphs)) {
                                            ProblemParagraphEntity problemParagraph = new ProblemParagraphEntity();
                                            problemParagraph.setDatasetId(datasetId);
                                            problemParagraph.setParagraphId(paragraph.getId());
                                            problemParagraph.setDocumentId(doc.getId());
                                            problemParagraph.setProblemId(problemId);
                                            problemParagraphs.add(problemParagraph);
                                        }
                                    }
                                }
                            }
                        })).doRead();
                docs.add(doc);
            }
            documentService.saveBatch(docs);
            paragraphService.saveBatch(paragraphs);
            problemService.saveBatch(problemEntities);
            problemParagraphMappingService.saveBatch(problemParagraphs);
        }
    }

    private boolean isExistProblemParagraph(UUID paragraphId, UUID problemId, List<ProblemParagraphEntity> problemParagraphs) {
        if (CollectionUtils.isEmpty(problemParagraphs)) {
            return false;
        }
        return problemParagraphs.stream().anyMatch(e -> problemId.equals(e.getProblemId()) && paragraphId.equals(e.getParagraphId()));
    }

    private ProblemEntity findProblem(String problem, List<ProblemEntity> problemEntities, List<ProblemEntity> dbProblemEntities) {
        if (!CollectionUtils.isEmpty(dbProblemEntities)) {
            return dbProblemEntities.stream()
                    .filter(e -> e.getContent().equals(problem))
                    .findFirst()
                    .orElse(null);
        }
        if (!CollectionUtils.isEmpty(problemEntities)) {
            return problemEntities.stream()
                    .filter(e -> e.getContent().equals(problem))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    @NotNull
    private ParagraphEntity getParagraphEntity(UUID datasetId, String title, String content,UUID docId) {
        ParagraphEntity paragraph = new ParagraphEntity();
        paragraph.setId(UUID.randomUUID());
        paragraph.setTitle(title== null ? "" : title);
        paragraph.setContent(content == null ? "" : content);
        paragraph.setDatasetId(datasetId);
        paragraph.setStatus("nn2");
        paragraph.setHitNum(0);
        paragraph.setIsActive(true);
        paragraph.setStatusMeta(new JSONObject());
        paragraph.setDocumentId(docId);
        return paragraph;
    }

    @Transactional
    public void importQa(UUID datasetId, MultipartFile[] file) throws IOException {
        for (MultipartFile uploadFile : file) {
            String fileName = uploadFile.getOriginalFilename();
            if (fileName != null && fileName.toLowerCase().endsWith(".zip")) {
                try (InputStream fis = uploadFile.getInputStream();
                     ZipArchiveInputStream zipIn = new ZipArchiveInputStream(fis)) {
                    ArchiveEntry entry;
                    while ((entry = zipIn.getNextEntry()) != null) {
                        // 假设ZIP包内只有一个文件或主要处理第一个找到的Excel文件
                        if (!entry.isDirectory() && (entry.getName().toLowerCase().endsWith(".xls") || entry.getName().endsWith(".xlsx") || entry.getName().endsWith(".csv"))) {
                            processExcelFile(datasetId, IOUtils.toByteArray(zipIn));
                            break; // 如果只处理一个文件，则在此处跳出循环
                        }
                    }
                }
            } else {
                processExcelFile(datasetId, uploadFile.getBytes());
            }
        }
    }

    @Transactional
    public boolean deleteDoc(UUID docId) {
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery().select(ProblemParagraphEntity::getProblemId).eq(ProblemParagraphEntity::getDocumentId, docId).list();
        problemParagraphService.lambdaUpdate().eq(ProblemParagraphEntity::getDocumentId, docId).update();
        paragraphService.lambdaUpdate().eq(ParagraphEntity::getDocumentId, docId).update();
        if (!CollectionUtils.isEmpty(list)) {
            problemService.removeBatchByIds(list.stream().map(ProblemParagraphEntity::getProblemId).toList());
        }
        return documentService.removeById(docId);
    }

    @Transactional
    public Boolean paragraphMigrate(UUID sourceDatasetId, UUID sourceDocId, UUID targetDatasetId, UUID targetDocId, List<UUID> paragraphIds) {
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery()
                .select(ProblemParagraphEntity::getProblemId)
                .in(ProblemParagraphEntity::getParagraphId, paragraphIds)
                .list();
        if (!CollectionUtils.isEmpty(list)) {
            problemService.lambdaUpdate()
                    .in(ProblemEntity::getId, list.stream().map(ProblemParagraphEntity::getProblemId).toList())
                    .set(ProblemEntity::getDatasetId, targetDatasetId).update();
        }
        problemParagraphService.lambdaUpdate()
                .in(ProblemParagraphEntity::getParagraphId, paragraphIds)
                .set(ProblemParagraphEntity::getDatasetId, targetDatasetId)
                .set(ProblemParagraphEntity::getDocumentId, targetDocId)
                .update();
        paragraphService.lambdaUpdate()
                .in(ParagraphEntity::getId, paragraphIds)
                .set(ParagraphEntity::getDatasetId, targetDatasetId)
                .set(ParagraphEntity::getDocumentId, targetDocId)
                .update();
        List<ParagraphEntity> sourceDocParagraphs = paragraphService.lambdaQuery()
                .select(ParagraphEntity::getContent)
                .eq(ParagraphEntity::getDocumentId, sourceDocId)
                .list();
        DocumentEntity sourceDoc = documentService.getById(sourceDocId);
        sourceDoc.setCharLength(0);
        for (ParagraphEntity paragraph : sourceDocParagraphs) {
            sourceDoc.setCharLength(sourceDoc.getCharLength() + paragraph.getContent().length());
        }
        List<ParagraphEntity> targetDocParagraphs = paragraphService.lambdaQuery()
                .select(ParagraphEntity::getContent)
                .eq(ParagraphEntity::getDocumentId, targetDocId)
                .list();
        DocumentEntity targetDoc = documentService.getById(targetDocId);
        targetDoc.setCharLength(0);
        for (ParagraphEntity paragraph : targetDocParagraphs) {
            targetDoc.setCharLength(targetDoc.getCharLength() + paragraph.getContent().length());
        }
        return documentService.updateById(sourceDoc) && documentService.updateById(targetDoc);
    }

    public Boolean paragraphBatchGenerateRelated(UUID datasetId, UUID docId, GenerateProblemDTO dto) {
        paragraphService.updateStatusByDocIds(List.of(docId), 2, 0);
        documentService.updateStatusMetaByIds(List.of(docId));
        documentService.updateStatusByIds(List.of(docId), 2, 0);
        List<ParagraphEntity> paragraphs = paragraphService.listByIds(dto.getParagraph_id_list());
        problemService.batchGenerateRelated(datasetId, docId, paragraphs, dto);
        return true;
    }

    @Transactional
    public void importTable(UUID datasetId, MultipartFile[] file) throws IOException {
        for (MultipartFile uploadFile : file) {
            System.out.println(uploadFile.getOriginalFilename());
            List<String> list = new ArrayList<>();
            EasyExcel.read(uploadFile.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                Map<Integer, String> headMap=new LinkedHashMap<>();
                // 表头信息会在此方法中获取
                @Override
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                    this.headMap=headMap;
                }
                // 每一行数据都会调用此方法
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    StringBuilder sb = new StringBuilder();
                    for (Integer i : data.keySet()) {
                        String value =data.get(i)==null?"":data.get(i);
                        sb.append(headMap.get(i)).append(":").append(value).append(";");
                    }
                    sb.deleteCharAt(sb.length()-1);
                    list.add(sb.toString());
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    log.info("所有数据解析完成！");
                }
            }).sheet().doRead();
            List<ParagraphEntity> paragraphs = new ArrayList<>();
            DocumentEntity doc = createDocument(datasetId, uploadFile.getOriginalFilename());
            if(!CollectionUtils.isEmpty(list)){
                for (String text : list) {
                    doc.setCharLength(doc.getCharLength()+text.length());
                    ParagraphEntity paragraph = getParagraphEntity(datasetId, "",text, doc.getId());
                    paragraphs.add(paragraph);
                }
                documentService.save(doc);
                paragraphService.saveBatch(paragraphs);
            }
        }
    }

    public List<TextSegmentVO> split(MultipartFile[] file) {
        return Collections.emptyList();
    }
}
