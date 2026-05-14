package com.maxkb4j.knowledge.handler;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.listener.ExcelDataListener;
import com.maxkb4j.knowledge.dto.ParagraphSimple;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import com.maxkb4j.knowledge.service.DocumentWriteService;
import com.maxkb4j.knowledge.service.KnowledgeService;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 知识库导入处理器
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeImportHandler {

    private final KnowledgeService knowledgeService;
    private final DocumentWriteService documentWriteService;
    private final ModelService modelService;

    /**
     * 从ZIP文件导入知识库
     * ZIP文件包含:
     * - knowledge.json: 知识库基本信息
     * - knowledge.xlsx: 知识库内容
     *
     * @param file ZIP文件
     * @return 创建的知识库实体
     */
    public KnowledgeEntity importKnowledgeFromZip(MultipartFile file) throws IOException {
        JSONObject knowledgeJson = null;
        List<KnowledgeExcel> excelDataList = new ArrayList<>();
        
        // 解析ZIP文件
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryName = entry.getName();
                if ("knowledge.json".equals(entryName)) {
                    // 读取知识库配置
                    String jsonContent = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    knowledgeJson = JSONObject.parseObject(jsonContent);
                } else if ("knowledge.xlsx".equals(entryName)) {
                    // 读取Excel数据
                    byte[] excelBytes = zis.readAllBytes();
                    excelDataList = readExcelData(new ByteArrayInputStream(excelBytes));
                }
            }
        }
        
        if (knowledgeJson == null) {
            throw new IllegalArgumentException("ZIP文件中未找到knowledge.json");
        }
        
        // 创建知识库
        KnowledgeEntity knowledge = createKnowledge(knowledgeJson);
        
        // 创建文档和段落
        if (!excelDataList.isEmpty()) {
            createDocuments(knowledge.getId(), excelDataList);
        }
        
        return knowledge;
    }

    /**
     * 从ZIP文件导入知识库（提供知识库ID）
     *
     * @param file     ZIP文件
     * @param knowledgeId 知识库ID
     */
    public void importKnowledgeFromZip(MultipartFile file, String knowledgeId) throws IOException {
        List<KnowledgeExcel> excelDataList = new ArrayList<>();
        
        // 解析ZIP文件
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryName = entry.getName();
                if ("knowledge.xlsx".equals(entryName)) {
                    // 读取Excel数据
                    byte[] excelBytes = zis.readAllBytes();
                    excelDataList = readExcelData(new ByteArrayInputStream(excelBytes));
                }
            }
        }
        
        // 创建文档和段落
        if (!excelDataList.isEmpty()) {
            createDocuments(knowledgeId, excelDataList);
        }
    }

    /**
     * 读取Excel数据
     */
    private List<KnowledgeExcel> readExcelData(InputStream inputStream) {
        ExcelDataListener<KnowledgeExcel> listener = new ExcelDataListener<>();
        EasyExcel.read(inputStream, KnowledgeExcel.class, listener).sheet().doRead();
        return listener.getDataList();
    }

    /**
     * 创建知识库
     */
    private KnowledgeEntity createKnowledge(JSONObject knowledgeJson) {
        KnowledgeEntity knowledge = new KnowledgeEntity();
        knowledge.setName(knowledgeJson.getString("name"));
        knowledge.setDesc(knowledgeJson.getString("desc"));
        knowledge.setType(knowledgeJson.getInteger("type"));
        
        JSONObject meta = knowledgeJson.getJSONObject("meta");
        if (meta != null) {
            knowledge.setMeta(meta);
        } else {
            knowledge.setMeta(new JSONObject());
        }
        
        Integer fileSizeLimit = knowledgeJson.getInteger("file_size_limit");
        if (fileSizeLimit != null) {
            knowledge.setFileSizeLimit(fileSizeLimit);
        } else {
            knowledge.setFileSizeLimit(100);
        }
        
        Integer fileCountLimit = knowledgeJson.getInteger("file_count_limit");
        if (fileCountLimit != null) {
            knowledge.setFileCountLimit(fileCountLimit);
        } else {
            knowledge.setFileCountLimit(50);
        }
        
        // 设置embedding_model_id，默认为向量模型的第一个
        String embeddingModelId = knowledgeJson.getString("embedding_model_id");
        if (embeddingModelId == null || embeddingModelId.isEmpty()) {
            embeddingModelId = getDefaultEmbeddingModelId();
        }
        knowledge.setEmbeddingModelId(embeddingModelId);
        knowledge.setFolderId("default");
        
        return knowledgeService.createKnowledge(knowledge);
    }

    /**
     * 创建文档和段落
     */
    private void createDocuments(String knowledgeId, List<KnowledgeExcel> excelDataList) {
        if (excelDataList.isEmpty()) {
            return;
        }
        
        // 按sheet名分组（这里简化处理，只创建一个文档）
        List<DocumentSimple> docs = new ArrayList<>();
        DocumentSimple doc = new DocumentSimple();
        doc.setName("knowledge.xlsx");
        
        List<ParagraphSimple> paragraphs = new ArrayList<>();
        for (KnowledgeExcel excel : excelDataList) {
            ParagraphSimple paragraph = ParagraphSimple.builder()
                    .title(excel.getTitle())
                    .content(excel.getContent())
                    .build();
            
            // 处理问题列表（多个问题用换行分隔）
            if (excel.getProblems() != null && !excel.getProblems().isEmpty()) {
                String[] problems = excel.getProblems().split("\n");
                List<String> problemList = new ArrayList<>();
                for (String problem : problems) {
                    String trimmed = problem.trim();
                    if (!trimmed.isEmpty()) {
                        problemList.add(trimmed);
                    }
                }
                paragraph.setProblemList(problemList);
            }
            
            paragraphs.add(paragraph);
        }
        doc.setParagraphs(paragraphs);
        docs.add(doc);
        
        documentWriteService.batchCreateDocs(knowledgeId, 0, docs);
    }
    
    /**
     * 获取默认向量模型ID（按创建时间倒序，取第一个）
     */
    private String getDefaultEmbeddingModelId() {
        ModelEntity embeddingModel = modelService.lambdaQuery()
                .eq(ModelEntity::getModelType, ModelType.EMBEDDING.getKey())
                .orderByDesc(ModelEntity::getCreateTime)
                .last("limit 1")
                .one();
        return embeddingModel != null ? embeddingModel.getId() : null;
    }
}
