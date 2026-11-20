package com.maxkb4j.knowledge.handler;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.dto.ParagraphSimple;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import com.maxkb4j.knowledge.listener.ExcelDataListener;
import com.maxkb4j.knowledge.service.DocumentWriteService;
import com.maxkb4j.knowledge.service.IKnowledgeInternalService;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.IModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final IKnowledgeInternalService knowledgeService;
    private final DocumentWriteService documentWriteService;
    private final IModelService modelService;
    private final UserContext userContext;


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
        Map<String, List<KnowledgeExcel>> excelDataBySheet = new LinkedHashMap<>();

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
                    excelDataBySheet = readExcelData(new ByteArrayInputStream(excelBytes));
                }
            }
        }

        if (knowledgeJson == null) {
            throw new IllegalArgumentException("ZIP文件中未找到knowledge.json");
        }

        // 创建知识库
        KnowledgeEntity knowledge = createKnowledge(knowledgeJson);

        // 创建文档和段落
        if (!excelDataBySheet.isEmpty()) {
            createDocuments(knowledge.getId(), excelDataBySheet);
        }

        return knowledge;
    }

    /**
     * 从ZIP文件导入知识库（提供知识库ID）
     *
     * @param file        ZIP文件
     * @param knowledgeId 知识库ID
     */
    public void importKnowledgeFromZip(MultipartFile file, String knowledgeId) throws IOException {
        Map<String, List<KnowledgeExcel>> excelDataBySheet = new LinkedHashMap<>();

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
                    excelDataBySheet = readExcelData(new ByteArrayInputStream(excelBytes));
                }
            }
        }

        // 创建文档和段落
        if (!excelDataBySheet.isEmpty()) {
            createDocuments(knowledgeId, excelDataBySheet);
        }
    }

    /**
     * 读取Excel数据
     */
    private Map<String, List<KnowledgeExcel>> readExcelData(InputStream inputStream) {
        Map<String, List<KnowledgeExcel>> excelDataBySheet = new LinkedHashMap<>();
        ExcelDataListener<KnowledgeExcel> listener = new ExcelDataListener<>();
        try (ExcelReader excelReader = EasyExcel.read(inputStream, KnowledgeExcel.class, listener).build()) {
            for (ReadSheet sheet : excelReader.excelExecutor().sheetList()) {
                excelReader.read(sheet);
                excelDataBySheet.put(sheet.getSheetName(), new ArrayList<>(listener.getDataList()));
                listener.clear();
            }
        }
        return excelDataBySheet;
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
        knowledge.setMeta(Objects.requireNonNullElseGet(meta, JSONObject::new));

        Integer fileSizeLimit = knowledgeJson.getInteger("fileSizeLimit");
        knowledge.setFileSizeLimit(Objects.requireNonNullElse(fileSizeLimit, 100));

        Integer fileCountLimit = knowledgeJson.getInteger("fileCountLimit");
        knowledge.setFileCountLimit(Objects.requireNonNullElse(fileCountLimit, 50));


        String embeddingModelId = knowledgeJson.getString("embeddingModelId");
        // 设置embedding_model_id，默认为向量模型的第一个
        knowledge.setEmbeddingModelId(modelService.getSafeModelId(embeddingModelId, ModelType.EMBEDDING));
        knowledge.setFolderId("default");

        return knowledgeService.createKnowledge(knowledge);
    }

    /**
     * 创建文档和段落
     */
    private void createDocuments(String knowledgeId, Map<String, List<KnowledgeExcel>> excelDataBySheet) {
        List<DocumentSimple> docs = new ArrayList<>();
        for (Map.Entry<String, List<KnowledgeExcel>> sheetData : excelDataBySheet.entrySet()) {
            if (sheetData.getValue().isEmpty()) {
                continue;
            }
            DocumentSimple doc = new DocumentSimple();
            doc.setName(sheetData.getKey());
            List<ParagraphSimple> paragraphs = new ArrayList<>();
            for (KnowledgeExcel excel : sheetData.getValue()) {
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
        }

        if (!docs.isEmpty()) {
            documentWriteService.batchCreateDocs(knowledgeId, KnowledgeType.BASE, docs);
        }
    }


}
