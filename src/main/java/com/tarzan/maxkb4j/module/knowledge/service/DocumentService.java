package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.ExcelUtil;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.core.event.DocumentIndexEvent;
import com.tarzan.maxkb4j.core.event.GenerateProblemEvent;
import com.tarzan.maxkb4j.listener.DataListener;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.*;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.DocumentVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.FileStreamVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextSegmentVO;
import com.tarzan.maxkb4j.module.knowledge.enums.KnowledgeType;
import com.tarzan.maxkb4j.module.knowledge.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.knowledge.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService extends ServiceImpl<DocumentMapper, DocumentEntity> {

    private final ParagraphService paragraphService;
    private final ProblemService problemService;
    private final ProblemParagraphService problemParagraphService;
    private final DocumentParseService documentParseService;
    private final DocumentSpiltService documentSpiltService;
    private final MongoFileService mongoFileService;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentWebService documentWebService;
    private final DocumentWriteService documentWriteService;

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
    public boolean migrateDoc(String sourceKnowledgeId, String targetKnowledgeId, List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        paragraphService.migrateDoc(sourceKnowledgeId, targetKnowledgeId, docIds);
        return this.lambdaUpdate()
                .set(DocumentEntity::getKnowledgeId, targetKnowledgeId)
                .in(DocumentEntity::getId, docIds)
                .update();
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

    @Transactional
    public void importQa(String knowledgeId, MultipartFile[] files) throws IOException {
        if (files == null) return;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String fileName = file.getOriginalFilename();
            if (fileName == null) continue;
            if (fileName.toLowerCase().endsWith(".zip")) {
                processZipQaFile(knowledgeId, file);
            } else {
                processQaFile(knowledgeId, file.getInputStream(), fileName);
            }
        }
    }

    @Transactional
    public void importTable(String knowledgeId, MultipartFile[] files) throws IOException {
        if (files == null) return;
        List<String> docIds = new ArrayList<>();
        for (MultipartFile uploadFile : files) {
            if (uploadFile == null || uploadFile.isEmpty()) continue;
            String originalFilename = uploadFile.getOriginalFilename();
            if (originalFilename == null) continue;
            List<String> list = documentParseService.extractTable(uploadFile.getInputStream());
            List<ParagraphEntity> paragraphs = new ArrayList<>();
            DocumentEntity doc = new DocumentEntity(knowledgeId, originalFilename, KnowledgeType.BASE.getType());
            if (!CollectionUtils.isEmpty(list)) {
                for (String text : list) {
                    doc.setCharLength(doc.getCharLength() + text.length());
                    ParagraphEntity paragraph = paragraphService.createParagraph(knowledgeId, doc.getId(), "", text, null);
                    paragraphs.add(paragraph);
                }
                doc.setMeta(upload(uploadFile));
                this.save(doc);
                paragraphService.saveBatch(paragraphs);
            }
            docIds.add(doc.getId());
        }
        eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, List.of("0")));
    }

    private JSONObject upload(MultipartFile file) throws IOException {
        String fileId = mongoFileService.storeFile(file);
        return new JSONObject(Map.of("allow_download", true, "sourceFileId", fileId));
    }

    @Transactional
    protected void processZipQaFile(String knowledgeId, MultipartFile zipFile) throws IOException {
        try (InputStream fis = zipFile.getInputStream();
             ZipArchiveInputStream zipIn = new ZipArchiveInputStream(fis)) {
            ArchiveEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (!entry.isDirectory() && isExcelOrCsv(entry.getName())) {
                    byte[] content = zipIn.readAllBytes();
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(content)) {
                        processQaFile(knowledgeId, bis, entry.getName());
                    }
                    break;
                }
            }
        }
    }

    private boolean isExcelOrCsv(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv");
    }

    @Transactional
    protected void processQaFile(String knowledgeId, InputStream fis, String fileName) {
        List<DocumentSimple> docs = new ArrayList<>();
        DataListener<DatasetExcel> dataListener = new DataListener<>();
        String fileId = mongoFileService.storeFile(fis, fileName,null);
        try (ExcelReader excelReader = EasyExcel.read(fis, DatasetExcel.class, dataListener).build()) {
            List<ReadSheet> sheets = excelReader.excelExecutor().sheetList();
            for (ReadSheet sheet : sheets) {
                DocumentSimple docSimple = new DocumentSimple();
                String sheetName = StringUtils.defaultIfBlank(sheet.getSheetName(), fileName);
                docSimple.setName(sheetName);
                docSimple.setSourceFileId(fileId);
                List<ParagraphSimple> paragraphs = new ArrayList<>();
                log.info("正在读取 Sheet: {}", sheet.getSheetName());
                excelReader.read(sheet);
                List<DatasetExcel> dataList = dataListener.getDataList();
                for (DatasetExcel data : dataList) {
                    log.info("在Sheet {} 中读取到一条数据: {}", sheet.getSheetName(), JSON.toJSONString(data));
                    ParagraphSimple paragraph = ParagraphSimple.builder().title(data.getTitle()).content(data.getContent()).build();
                    if (StringUtils.isNotBlank(data.getProblems())) {
                        String[] problems = data.getProblems().split("\n");
                        paragraph.setProblemList(Arrays.asList(problems));
                    }
                    paragraphs.add(paragraph);
                }
                docSimple.setParagraphs(paragraphs);
                docs.add(docSimple);
                dataListener.clear();
            }
        } catch (Exception e) {
            log.error("读取 Excel 失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取 Excel 失败", e);
        }
        batchCreateDocs(knowledgeId,KnowledgeType.BASE.getType(), docs);
    }

    public boolean batchCreateDocs(String knowledgeId,int knowledgeType, List<DocumentSimple> docs) {
       return documentWriteService.batchCreateDocs(knowledgeId,knowledgeType, docs);
    }

    public void exportExcelByDocId(String docId, HttpServletResponse response) {
        DocumentEntity doc = this.getById(docId);
        if (doc == null) return;
        List<DatasetExcel> list = getDatasetExcelByDoc(doc);
        ExcelUtil.export(response, doc.getName(), doc.getName(), list, DatasetExcel.class);
    }

    public void exportExcelZipByDocId(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = this.getById(docId);
        if (doc == null) return;
        exportExcelZipByDocs(List.of(doc), doc.getName(), response);
    }

    private List<DatasetExcel> getDatasetExcelByDoc(DocumentEntity doc) {
        List<DatasetExcel> list = new ArrayList<>();
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery()
                .eq(ParagraphEntity::getDocumentId, doc.getId())
                .list();
        for (ParagraphEntity paragraph : paragraphs) {
            DatasetExcel excel = new DatasetExcel();
            excel.setTitle(paragraph.getTitle());
            excel.setContent(paragraph.getContent());
            List<ProblemEntity> problemEntities = problemParagraphService.getProblemsByParagraphId(paragraph.getId());
            if (!CollectionUtils.isEmpty(problemEntities)) {
                String problems = problemEntities.stream()
                        .map(ProblemEntity::getContent)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining("\n"));
                excel.setProblems(problems);
            }
            list.add(excel);
        }
        return list;
    }

    public void exportExcelZipByDocs(List<DocumentEntity> docs, String exportName, HttpServletResponse response) throws
            IOException {
        if (docs.isEmpty()) return;
        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encodedName = URLEncoder.encode(exportName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + encodedName + ".zip");
        try (ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(zipBuffer);
             ByteArrayOutputStream excelBuffer = new ByteArrayOutputStream();
             ExcelWriter excelWriter = EasyExcel.write(excelBuffer, DatasetExcel.class).build()) {
            for (DocumentEntity doc : docs) {
                List<DatasetExcel> data = getDatasetExcelByDoc(doc);
                WriteSheet sheet = EasyExcel.writerSheet(doc.getName()).build();
                excelWriter.write(data, sheet);
            }
            excelWriter.finish();

            ZipEntry zipEntry = new ZipEntry(exportName + ".xlsx");
            zipOut.putNextEntry(zipEntry);
            zipOut.write(excelBuffer.toByteArray());
            zipOut.closeEntry();
            zipOut.finish();

            IoUtil.copy(new ByteArrayInputStream(zipBuffer.toByteArray()), response.getOutputStream());
        }
    }


    @Transactional
    public boolean deleteBatchDocByDocIds(String knowledgeId, List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        paragraphService.deleteByDocIds(knowledgeId, docIds);
        return this.lambdaUpdate().in(DocumentEntity::getId, docIds).remove();
    }

    @Transactional
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

    @Transactional
    public boolean deleteDoc(String docId) {
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery()
                .select(ProblemParagraphEntity::getProblemId)
                .eq(ProblemParagraphEntity::getDocumentId, docId)
                .list();
        problemParagraphService.lambdaUpdate().eq(ProblemParagraphEntity::getDocumentId, docId).remove();
        paragraphService.lambdaUpdate().eq(ParagraphEntity::getDocumentId, docId).remove();
        if (!CollectionUtils.isEmpty(list)) {
            List<String> problemIds = list.stream().map(ProblemParagraphEntity::getProblemId).distinct().toList();
            problemService.removeBatchByIds(problemIds);
        }
        return this.removeById(docId);
    }

    public IPage<DocumentVO> getDocByKnowledgeId(String knowledgeId, int current, int size, DocQuery query) {
        Page<DocumentVO> docPage = new Page<>(current, size);
        baseMapper.selectDocPage(docPage, knowledgeId, query);
        return docPage;
    }

    public List<KeyAndValueVO> splitPattern() {
        return Arrays.asList(
                new KeyAndValueVO("#", "(?<=^)# .*|(?<=\\n)# .*"),
                new KeyAndValueVO("##", "(?<=\\n)(?<!#)## (?!#).*|(?<=^)(?<!#)## (?!#).*"),
                new KeyAndValueVO("###", "(?<=\\n)(?<!#)### (?!#).*|(?<=^)(?<!#)### (?!#).*"),
                new KeyAndValueVO("####", "(?<=\\n)(?<!#)#### (?!#).*|(?<=^)(?<!#)#### (?!#).*"),
                new KeyAndValueVO("#####", "(?<=\\n)(?<!#)##### (?!#).*|(?<=^)(?<!#)##### (?!#).*"),
                new KeyAndValueVO("######", "(?<=\\n)(?<!#)###### (?!#).*|(?<=^)(?<!#)###### (?!#).*"),
                new KeyAndValueVO("-", "(?<! )- .*"),
                new KeyAndValueVO("space", "(?<! ) (?! )"),
                new KeyAndValueVO("semicolon", "(?<!；)；(?!；)"),
                new KeyAndValueVO("comma", "(?<!，)，(?!，)"),
                new KeyAndValueVO("period", "(?<!。)。(?!。)"),
                new KeyAndValueVO("enter", "(?<!\\n)\\n(?!\\n)"),
                new KeyAndValueVO("blank line", "(?<!\\n)\\n\\n(?!\\n)")
        );
    }

    public List<TextSegmentVO> split(MultipartFile[] files, String[] patterns, Integer limit, Boolean withFilter) throws IOException {
        List<TextSegmentVO> result = new ArrayList<>();
        List<FileStreamVO> fileStreams = new ArrayList<>();
        if (files == null) return result;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String name = file.getOriginalFilename();
            if (name == null) continue;
            if (name.toLowerCase().endsWith(".zip")) {
                try (ZipArchiveInputStream zis = new ZipArchiveInputStream(file.getInputStream())) {
                    ZipArchiveEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            byte[] bytes = zis.readAllBytes();
                            InputStream inputStream = new ByteArrayInputStream(bytes);
                            fileStreams.add(new FileStreamVO(entry.getName(), inputStream, ""));
                        }
                    }
                }
            } else {
                fileStreams.add(new FileStreamVO(name, file.getInputStream(), file.getContentType()));
            }
        }
        for (FileStreamVO fs : fileStreams) {
            TextSegmentVO vo = new TextSegmentVO();
            vo.setName(fs.getName());
            String text = documentParseService.extractText(fs.getName(), fs.getInputStream());
            vo.setContent(documentSpiltService.split(text, patterns, limit, withFilter));
            String fileId = mongoFileService.storeFile(fs.getInputStream(), fs.getName(), fs.getContentType());
            vo.setSourceFileId(fileId);
            result.add(vo);
        }
        return result;
    }

    @Async
    public void createWebDocs(String knowledgeId, String sourceUrl, String selector) {
        List<DocumentSimple> docs =documentWebService.getWebDocuments(sourceUrl, selector,true);
        batchCreateDocs(knowledgeId, KnowledgeType.WEB.getType(), docs);
    }


    @Transactional
    public void createWebDoc(String knowledgeId, List<String> sourceUrlList, String selector) {
        for (String sourceUrl : sourceUrlList) {
            List<DocumentSimple> docs =documentWebService.getWebDocuments(sourceUrl, selector,false);
            batchCreateDocs(knowledgeId, KnowledgeType.WEB.getType(), docs);
        }
    }


    @Transactional
    public void syncWebDoc(String knowledgeId, String docId) {
        DocumentEntity doc = this.getById(docId);
        if (doc == null || doc.getMeta() == null) return;
        String sourceUrl = doc.getMeta().getString("sourceUrl");
        String selector = doc.getMeta().getString("selector");
        if (StringUtils.isAnyBlank(sourceUrl, selector)) return;
        deleteBatchDocByDocIds(knowledgeId, List.of(docId));
        List<DocumentSimple> docs =documentWebService.getWebDocuments(sourceUrl, selector,false);
        batchCreateDocs(knowledgeId, KnowledgeType.WEB.getType(), docs);
    }



    public boolean batchGenerateRelated(String knowledgeId, GenerateProblemDTO dto) {
        eventPublisher.publishEvent(new GenerateProblemEvent(this, knowledgeId, dto.getDocumentIdList(), dto.getModelId(), dto.getPrompt(), dto.getStateList()));
        return true;
    }

    public boolean downloadSourceFile(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = this.getById(docId);
        if (doc == null || doc.getMeta() == null) return false;

        String fileId = doc.getMeta().getString("sourceFileId");
        if (StringUtils.isBlank(fileId)) return false;

        try (InputStream in = mongoFileService.getStream(fileId)) {
            IoUtil.copy(in, response.getOutputStream());
            return true;
        }
    }

    // ===== 封装事件发布 =====
    private void publishDocumentIndexEvent(String knowledgeId, List<String> docIds, List<String> stateList) {
        if (!docIds.isEmpty()) {
            eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, stateList));
        }
    }
}