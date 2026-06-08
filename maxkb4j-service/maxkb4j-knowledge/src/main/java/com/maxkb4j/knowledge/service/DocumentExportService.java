package com.maxkb4j.knowledge.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.core.util.ExcelUtil;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.oss.service.IOssService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
@Component
@RequiredArgsConstructor
public class DocumentExportService {

    private final DocumentService documentService;
    private final ParagraphService paragraphService;
    private final ProblemParagraphService problemParagraphService;
    private final IOssService mongoFileService;
    private final KnowledgeMapper knowledgeMapper;

    public void exportExcelByDocIds(String knowledgeId, List<String> docIds, HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<KnowledgeEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.select(KnowledgeEntity::getName).eq(KnowledgeEntity::getId, knowledgeId);
        KnowledgeEntity knowledge = knowledgeMapper.selectOne(queryWrapper);
        List<DocumentEntity> docs = documentService.lambdaQuery().select(DocumentEntity::getId, DocumentEntity::getName).in(DocumentEntity::getId, docIds).list();

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(knowledge.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), KnowledgeExcel.class).build()) {
            for (DocumentEntity doc : docs) {
                List<KnowledgeExcel> list = getDatasetExcelByDocId(doc.getId());
                String sheetName = doc.getName();
                int index = sheetName.lastIndexOf(".");
                if (index > 0) {
                    sheetName = sheetName.substring(0, Math.min(31, index));
                }
                WriteSheet sheet = EasyExcel.writerSheet(sheetName).build();
                excelWriter.write(list, sheet);
            }
        }
    }

    public void exportExcelZipByDocIds(String knowledgeId, List<String> docIds, HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<KnowledgeEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.select(KnowledgeEntity::getName).eq(KnowledgeEntity::getId, knowledgeId);
        KnowledgeEntity knowledge = knowledgeMapper.selectOne(queryWrapper);
        List<DocumentEntity> docs = documentService.lambdaQuery().select(DocumentEntity::getId, DocumentEntity::getName).in(DocumentEntity::getId, docIds).list();
        if (docs.isEmpty()) return;

        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(knowledge.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".zip");
        try (ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(zipBuffer);
             ByteArrayOutputStream excelBuffer = new ByteArrayOutputStream();
             ExcelWriter excelWriter = EasyExcel.write(excelBuffer, KnowledgeExcel.class).build()) {
            for (DocumentEntity doc : docs) {
                List<KnowledgeExcel> data = getDatasetExcelByDocId(doc.getId());
                WriteSheet sheet = EasyExcel.writerSheet(doc.getName()).build();
                excelWriter.write(data, sheet);
            }
            excelWriter.finish();

            ZipEntry zipEntry = new ZipEntry(knowledge.getName() + ".xlsx");
            zipOut.putNextEntry(zipEntry);
            zipOut.write(excelBuffer.toByteArray());
            zipOut.closeEntry();
            zipOut.finish();

            IoUtil.copy(new ByteArrayInputStream(zipBuffer.toByteArray()), response.getOutputStream());
        }
    }


    public void exportExcelByDocId(String docId, HttpServletResponse response) {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null) return;
        List<KnowledgeExcel> list = getDatasetExcelByDocId(docId);
        int index = doc.getName().lastIndexOf(".");
        int end = Math.min(31, index);
        String sheetName = doc.getName().substring(0, end);
        ExcelUtil.export(response, doc.getName(), sheetName, list, KnowledgeExcel.class);
    }

    public void exportExcelZipByDocId(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null) return;
        exportExcelZipByDocs(List.of(doc), doc.getName(), response);
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
             ExcelWriter excelWriter = EasyExcel.write(excelBuffer, KnowledgeExcel.class).build()) {
            for (DocumentEntity doc : docs) {
                List<KnowledgeExcel> data = getDatasetExcelByDocId(doc.getId());
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

    public boolean downloadSourceFile(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null || doc.getMeta() == null) return false;

        String fileId = doc.getMeta().getString("sourceFileId");
        if (StringUtils.isBlank(fileId)) return false;

        try (InputStream in = mongoFileService.getStream(fileId)) {
            IoUtil.copy(in, response.getOutputStream());
            return true;
        }
    }

    public boolean replaceSourceFile(String id, String docId, MultipartFile file) throws IOException {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null) return false;
        String fileId = mongoFileService.storeFile(file);
        doc.setMeta(new JSONObject(Map.of("allow_download", true, "sourceFileId", fileId)));
        return documentService.updateById(doc);
    }

    private List<KnowledgeExcel> getDatasetExcelByDocId(String docId) {
        List<KnowledgeExcel> list = new ArrayList<>();
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery()
                .eq(ParagraphEntity::getDocumentId, docId)
                .list();
        for (ParagraphEntity paragraph : paragraphs) {
            KnowledgeExcel excel = new KnowledgeExcel();
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


}