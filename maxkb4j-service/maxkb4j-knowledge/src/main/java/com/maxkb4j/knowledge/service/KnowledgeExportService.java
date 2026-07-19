package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.handler.KnowledgeExportHandler;
import com.maxkb4j.knowledge.service.impl.DocumentServiceImpl;
import com.maxkb4j.knowledge.service.impl.KnowledgeServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 知识库导出服务
 * 负责知识库的 Excel / ZIP 导出编排
 *
 * @author tarzan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeExportService {

    private final KnowledgeServiceImpl knowledgeService;
    private final DocumentServiceImpl documentService;
    private final KnowledgeExportHandler knowledgeExportHandler;

    /**
     * 根据知识库ID获取文档列表
     */
    private List<DocumentEntity> getDocumentsByKnowledgeId(String knowledgeId) {
        return documentService.list(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getKnowledgeId, knowledgeId));
    }

    /**
     * 导出 ZIP 格式的 Excel
     */
    public void exportExcelZip(String id, HttpServletResponse response) throws IOException {
        KnowledgeEntity dataset = knowledgeService.getById(id);
        if (dataset == null) {
            throw new IllegalArgumentException("未找到知识库 ID: " + id);
        }
        List<DocumentEntity> docs = getDocumentsByKnowledgeId(id);
        if (docs == null || docs.isEmpty()) {
            throw new IllegalArgumentException("文档列表为空，无法导出");
        }
        knowledgeExportHandler.writeExcelToZipAndResponse(docs, dataset.getName(), response);
    }

    /**
     * 直接导出 Excel（不压缩）
     */
    public void exportExcel(String id, HttpServletResponse response) throws IOException {
        KnowledgeEntity dataset = knowledgeService.getById(id);
        if (dataset == null) {
            throw new IllegalArgumentException("未找到知识库 ID: " + id);
        }
        List<DocumentEntity> docs = getDocumentsByKnowledgeId(id);
        knowledgeExportHandler.setExcelResponseHeader(response, dataset.getName());
        knowledgeExportHandler.writeMultiSheetExcel(response.getOutputStream(), docs);
    }

    /**
     * 导出知识库ZIP包（包含knowledge.json和knowledge.xlsx）
     */
    public void exportKnowledge(String id, HttpServletResponse response) throws IOException {
        KnowledgeEntity knowledge = knowledgeService.getById(id);
        if (knowledge == null) {
            throw new IllegalArgumentException("未找到知识库 ID: " + id);
        }
        List<DocumentEntity> docs = getDocumentsByKnowledgeId(id);
        knowledgeExportHandler.exportKnowledgeZip(docs, knowledge.getName(), knowledge.getDesc(),
                knowledge.getType(), knowledge.getMeta(), knowledge.getFileSizeLimit(),
                knowledge.getFileCountLimit(), response);
    }
}
