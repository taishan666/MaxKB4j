package com.maxkb4j.knowledge.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.knowledge.support.ExcelExportSupport;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 知识文档导出编排：单/多文档 Excel 和 ZIP 导出。
 *
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentExportService {

    private final DocumentService documentService;
    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeExcelDataBuilder excelDataBuilder;

    public void exportExcel(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null) return;
        ExcelExportSupport.prepareExcelResponse(response, doc.getName());
        EasyExcel.write(response.getOutputStream(), KnowledgeExcel.class)
                .sheet(ExcelExportSupport.normalizeSheetName(doc.getName()))
                .doWrite(excelDataBuilder.buildByDocId(docId));
    }

    public void exportExcel(String knowledgeId, List<String> docIds, HttpServletResponse response) throws IOException {
        KnowledgeEntity knowledge = loadKnowledgeName(knowledgeId);
        List<DocumentEntity> docs = loadDocSummaries(docIds);
        if (docs.isEmpty()) return;

        ExcelExportSupport.prepareExcelResponse(response, knowledge.getName());
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream(), KnowledgeExcel.class).build()) {
            ExcelExportSupport.writeSheets(writer, docs, DocumentEntity::getName,
                    doc -> excelDataBuilder.buildByDocId(doc.getId()));
        }
    }

    public void exportZip(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null) return;
        writeZip(List.of(doc), doc.getName(), response);
    }

    public void exportZip(String knowledgeId, List<String> docIds, HttpServletResponse response) throws IOException {
        KnowledgeEntity knowledge = loadKnowledgeName(knowledgeId);
        List<DocumentEntity> docs = loadDocSummaries(docIds);
        writeZip(docs, knowledge.getName(), response);
    }

    private void writeZip(List<DocumentEntity> docs, String exportName, HttpServletResponse response) throws IOException {
        if (docs.isEmpty()) return;
        ExcelExportSupport.prepareZipResponse(response, exportName);
        ExcelExportSupport.writeZippedExcel(response.getOutputStream(), exportName, KnowledgeExcel.class,
                docs, DocumentEntity::getName, doc -> excelDataBuilder.buildByDocId(doc.getId()));
    }

    private KnowledgeEntity loadKnowledgeName(String knowledgeId) {
        return knowledgeMapper.selectOne(Wrappers.<KnowledgeEntity>lambdaQuery()
                .select(KnowledgeEntity::getName)
                .eq(KnowledgeEntity::getId, knowledgeId));
    }

    private List<DocumentEntity> loadDocSummaries(List<String> docIds) {
        return documentService.lambdaQuery()
                .select(DocumentEntity::getId, DocumentEntity::getName)
                .in(DocumentEntity::getId, docIds)
                .list();
    }
}
