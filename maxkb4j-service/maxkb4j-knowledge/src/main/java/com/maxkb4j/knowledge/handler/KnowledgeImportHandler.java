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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
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

    /** 单个 ZIP 条目解压后大小上限：100MB */
    private static final long MAX_ENTRY_UNCOMPRESSED_SIZE = 100L * 1024 * 1024;
    /** ZIP 全部条目解压后总大小上限：200MB，防止多个条目叠加构成解压炸弹 */
    private static final long MAX_TOTAL_UNCOMPRESSED_SIZE = 200L * 1024 * 1024;
    /** ZIP 条目数量上限 */
    private static final int MAX_ENTRY_COUNT = 1000;
    private static final int READ_BUFFER_SIZE = 8192;
    private static final String KNOWLEDGE_JSON_ENTRY = "knowledge.json";
    private static final String KNOWLEDGE_XLSX_ENTRY = "knowledge.xlsx";

    private final IKnowledgeInternalService knowledgeService;
    private final DocumentWriteService documentWriteService;
    private final IModelService modelService;
    private final UserContext userContext;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    private void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }


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
        ZipContent content = parseZip(file);

        if (content.knowledgeJson() == null) {
            throw new IllegalArgumentException("ZIP文件中未找到knowledge.json");
        }

        // 建库与建文档必须同生共死：文档创建失败时回滚知识库，避免残留空库。
        // 事务只覆盖数据库写入，ZIP/Excel 解析放在事务外，避免解析期间占用数据库连接。
        return transactionTemplate.execute(status -> {
            KnowledgeEntity knowledge = createKnowledge(content.knowledgeJson());
            if (!content.excelDataBySheet().isEmpty()) {
                createDocuments(knowledge.getId(), content.excelDataBySheet());
            }
            return knowledge;
        });
    }

    /**
     * 从ZIP文件导入知识库（提供知识库ID）
     *
     * @param file        ZIP文件
     * @param knowledgeId 知识库ID
     */
    public void importKnowledgeFromZip(MultipartFile file, String knowledgeId) throws IOException {
        ZipContent content = parseZip(file);

        // 创建文档和段落
        if (!content.excelDataBySheet().isEmpty()) {
            createDocuments(knowledgeId, content.excelDataBySheet());
        }
    }

    /**
     * 解析ZIP文件内容
     */
    private ZipContent parseZip(MultipartFile file) throws IOException {
        JSONObject knowledgeJson = null;
        Map<String, List<KnowledgeExcel>> excelDataBySheet = new LinkedHashMap<>();

        // 解析ZIP文件：全部条目有界读取，防止解压炸弹撑爆内存
        AtomicLong totalUncompressed = new AtomicLong();
        int entryCount = 0;
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (++entryCount > MAX_ENTRY_COUNT) {
                    throw new IllegalArgumentException("ZIP文件条目数超过上限：" + MAX_ENTRY_COUNT);
                }

                String entryName = entry.getName();
                byte[] entryBytes = readEntryBounded(zis, entryName, totalUncompressed);
                if (KNOWLEDGE_JSON_ENTRY.equals(entryName)) {
                    // 读取知识库配置
                    knowledgeJson = JSONObject.parseObject(new String(entryBytes, StandardCharsets.UTF_8));
                } else if (KNOWLEDGE_XLSX_ENTRY.equals(entryName)) {
                    // 读取Excel数据
                    excelDataBySheet = readExcelData(new ByteArrayInputStream(entryBytes));
                }
                // 其余条目读取后丢弃
            }
        }

        return new ZipContent(knowledgeJson, excelDataBySheet);
    }

    /**
     * 有界读取当前条目：单个条目及全包累计解压字节数任一超限立即失败，防止ZIP炸弹导致OOM
     */
    private byte[] readEntryBounded(ZipInputStream zis, String entryName, AtomicLong totalUncompressed) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        long entrySize = 0;
        int len;
        while ((len = zis.read(buffer)) != -1) {
            entrySize += len;
            if (entrySize > MAX_ENTRY_UNCOMPRESSED_SIZE) {
                throw new IllegalArgumentException(
                        "ZIP条目[" + entryName + "]解压后大小超过上限：" + MAX_ENTRY_UNCOMPRESSED_SIZE / (1024 * 1024) + "MB");
            }
            if (totalUncompressed.addAndGet(len) > MAX_TOTAL_UNCOMPRESSED_SIZE) {
                throw new IllegalArgumentException(
                        "ZIP文件解压后总大小超过上限：" + MAX_TOTAL_UNCOMPRESSED_SIZE / (1024 * 1024) + "MB");
            }
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    /**
     * 读取Excel数据
     */
    private Map<String, List<KnowledgeExcel>> readExcelData(InputStream inputStream) {
        Map<String, List<KnowledgeExcel>> excelDataBySheet = new LinkedHashMap<>();
        ExcelDataListener<KnowledgeExcel> listener = new ExcelDataListener<>();
        try (ExcelReader excelReader = EasyExcel.read(inputStream, KnowledgeExcel.class, listener).build()) {
            for (ReadSheet sheet : excelReader.excelExecutor().sheetList()) {
                // 只按 sheetNo 定位读取：带上 sheetName 时 EasyExcel 会在 sheetNo 未命中后按名字兜底匹配，
                // 同名 sheet 会交叉读取并在第二次读取时抛 Cannot read sheet repeatedly
                excelReader.read(new ReadSheet(sheet.getSheetNo()));
                // sheet 名直接作 key，同名 sheet（Excel 界面造不出，程序生成的文件可携带）会整表覆盖，须先消重
                excelDataBySheet.put(allocateDocName(sheet.getSheetName(), excelDataBySheet.keySet()),
                        new ArrayList<>(listener.getDataList()));
                listener.clear();
            }
        }
        return excelDataBySheet;
    }

    /**
     * sheet 名转文档名：与已有名冲突时追加 "(n)" 序号（与导出侧 ExcelExportSupport 的命名约定一致），
     * 避免同名 sheet 在 LinkedHashMap 中后者覆盖前者、整表数据静默丢失
     */
    private static String allocateDocName(String sheetName, Set<String> usedNames) {
        if (!usedNames.contains(sheetName)) {
            return sheetName;
        }
        for (int index = 2; ; index++) {
            String candidate = sheetName + "(" + index + ")";
            if (!usedNames.contains(candidate)) {
                return candidate;
            }
        }
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

    /**
     * ZIP解析结果
     */
    private record ZipContent(JSONObject knowledgeJson, Map<String, List<KnowledgeExcel>> excelDataBySheet) {
    }
}
