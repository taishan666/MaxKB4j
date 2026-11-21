package com.maxkb4j.knowledge.handler;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.dto.ParagraphSimple;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import com.maxkb4j.knowledge.service.DocumentWriteService;
import com.maxkb4j.knowledge.service.IKnowledgeInternalService;
import com.maxkb4j.model.service.IModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeImportHandlerTest {

    @Mock
    private IKnowledgeInternalService knowledgeService;
    @Mock
    private DocumentWriteService documentWriteService;
    @Mock
    private IModelService modelService;
    @Mock
    private UserContext userContext;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private MultipartFile file;
    @InjectMocks
    private KnowledgeImportHandler knowledgeImportHandler;
    @Captor
    private ArgumentCaptor<List<DocumentSimple>> docsCaptor;

    @BeforeEach
    void setUp() {
        // Mockito 构造器注入不会填充剩余字段，事务模板需手动装配
        ReflectionTestUtils.setField(knowledgeImportHandler, "transactionTemplate", transactionTemplate);
    }

    @Test
    void importKnowledgeFromZip_restoresAllSheetsAsDocuments() throws Exception {
        ByteArrayOutputStream excelOutput = new ByteArrayOutputStream();
        try (ExcelWriter excelWriter = EasyExcel.write(excelOutput, KnowledgeExcel.class).build()) {
            excelWriter.write(List.of(row("标题一", "内容一", "问题一")),
                    EasyExcel.writerSheet(0, "产品说明").build());
            excelWriter.write(List.of(
                            row("标题二", "内容二", "问题二\n问题三"),
                            row("标题三", "内容三", null)),
                    EasyExcel.writerSheet(1, "常见问题").build());
        }

        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipOutput)) {
            zip.putNextEntry(new ZipEntry("knowledge.xlsx"));
            zip.write(excelOutput.toByteArray());
            zip.closeEntry();
        }
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(zipOutput.toByteArray()));

        knowledgeImportHandler.importKnowledgeFromZip(file, "knowledge-1");

        verify(documentWriteService).batchCreateDocs(
                eq("knowledge-1"), eq(KnowledgeType.BASE), docsCaptor.capture());
        List<DocumentSimple> docs = docsCaptor.getValue();
        assertEquals(List.of("产品说明", "常见问题"),
                docs.stream().map(DocumentSimple::getName).toList());
        assertEquals(List.of("标题一"),
                docs.get(0).getParagraphs().stream().map(ParagraphSimple::getTitle).toList());
        assertEquals(List.of("标题二", "标题三"),
                docs.get(1).getParagraphs().stream().map(ParagraphSimple::getTitle).toList());
        assertEquals(List.of("问题二", "问题三"), docs.get(1).getParagraphs().get(0).getProblemList());
        assertEquals("内容三", docs.get(1).getParagraphs().get(1).getContent());
    }

    @Test
    void importKnowledgeFromZip_createsKnowledgeAndDocsInsideTransaction() throws Exception {
        stubTransactionExecute();
        byte[] excel = singleSheetExcel("产品说明", row("标题一", "内容一", null));
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(
                zipOf(Map.of("knowledge.json", knowledgeJson().toJSONString().getBytes(StandardCharsets.UTF_8), "knowledge.xlsx", excel))));
        when(knowledgeService.createKnowledge(any())).thenAnswer(inv -> {
            KnowledgeEntity knowledge = inv.getArgument(0);
            knowledge.setId("k-1");
            return knowledge;
        });

        KnowledgeEntity result = knowledgeImportHandler.importKnowledgeFromZip(file);

        assertEquals("k-1", result.getId());
        assertEquals("知识库A", result.getName());
        verify(documentWriteService).batchCreateDocs(eq("k-1"), eq(KnowledgeType.BASE), anyList());
    }

    @Test
    void importKnowledgeFromZip_withoutKnowledgeJson_rejected() throws Exception {
        byte[] excel = singleSheetExcel("产品说明", row("标题一", "内容一", null));
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(
                zipOf(Map.of("knowledge.xlsx", excel))));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> knowledgeImportHandler.importKnowledgeFromZip(file));

        assertEquals("ZIP文件中未找到knowledge.json", ex.getMessage());
        verify(knowledgeService, never()).createKnowledge(any());
        verify(documentWriteService, never()).batchCreateDocs(anyString(), anyInt(), anyList());
    }

    @Test
    void importKnowledgeFromZip_entryOverUncompressedLimit_rejected() throws Exception {
        // 单个条目解压后超过 100MB 上限的炸弹条目（压缩后很小，解压会撑爆内存）
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        try (ZipOutputStream zip = new ZipOutputStream(zipOutput)) {
            zip.putNextEntry(new ZipEntry("knowledge.json"));
            for (long written = 0; written < 100L * 1024 * 1024 + 1; written += chunk.length) {
                zip.write(chunk);
            }
            zip.closeEntry();
        }
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(zipOutput.toByteArray()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> knowledgeImportHandler.importKnowledgeFromZip(file));

        assertTrue(ex.getMessage().contains("解压后大小超过上限"));
        verify(knowledgeService, never()).createKnowledge(any());
        verify(documentWriteService, never()).batchCreateDocs(anyString(), anyInt(), anyList());
    }

    @Test
    void importKnowledgeFromZip_tooManyEntries_rejected() throws Exception {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipOutput)) {
            for (int i = 0; i <= 1000; i++) {
                zip.putNextEntry(new ZipEntry("pad-" + i + ".txt"));
                zip.write(1);
                zip.closeEntry();
            }
        }
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(zipOutput.toByteArray()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> knowledgeImportHandler.importKnowledgeFromZip(file));

        assertTrue(ex.getMessage().contains("条目数超过上限"));
        verify(knowledgeService, never()).createKnowledge(any());
    }

    @Test
    void importKnowledgeFromZip_docCreationFailure_propagatesForRollback() throws Exception {
        stubTransactionExecute();
        byte[] excel = singleSheetExcel("产品说明", row("标题一", "内容一", null));
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(
                zipOf(Map.of("knowledge.json", knowledgeJson().toJSONString().getBytes(StandardCharsets.UTF_8), "knowledge.xlsx", excel))));
        when(knowledgeService.createKnowledge(any())).thenAnswer(inv -> {
            KnowledgeEntity knowledge = inv.getArgument(0);
            knowledge.setId("k-1");
            return knowledge;
        });
        doThrow(new RuntimeException("文档创建失败")).when(documentWriteService)
                .batchCreateDocs(anyString(), anyInt(), anyList());

        assertThrows(RuntimeException.class, () -> knowledgeImportHandler.importKnowledgeFromZip(file));

        // 建库与建文档在同一个事务回调内执行：文档失败即抛出，由事务统一回滚知识库
        verify(knowledgeService).createKnowledge(any());
    }

    @Test
    void importKnowledgeFromZip_duplicateSheetNames_bothTablesImported() throws Exception {
        // Excel 界面造不出同名 sheet，但程序生成的 xlsx 可以携带：两张同名表不得后者覆盖前者
        ByteArrayOutputStream excelOutput = new ByteArrayOutputStream();
        try (ExcelWriter excelWriter = EasyExcel.write(excelOutput, KnowledgeExcel.class).build()) {
            excelWriter.write(List.of(row("标题一", "内容一", null)),
                    EasyExcel.writerSheet(0, "产品说明").build());
            excelWriter.write(List.of(row("标题二", "内容二", null)),
                    EasyExcel.writerSheet(1, "常见问题").build());
        }
        byte[] excel = renameSheet(excelOutput.toByteArray(), "常见问题", "产品说明");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(
                zipOf(Map.of("knowledge.xlsx", excel))));

        knowledgeImportHandler.importKnowledgeFromZip(file, "knowledge-1");

        verify(documentWriteService).batchCreateDocs(
                eq("knowledge-1"), eq(KnowledgeType.BASE), docsCaptor.capture());
        List<DocumentSimple> docs = docsCaptor.getValue();
        // 两张表都导入，重名的第二张追加序号区分，与导出侧命名约定一致
        assertEquals(List.of("产品说明", "产品说明(2)"),
                docs.stream().map(DocumentSimple::getName).toList());
        assertEquals(List.of("标题一"), docs.get(0).getParagraphs().stream().map(ParagraphSimple::getTitle).toList());
        assertEquals(List.of("标题二"), docs.get(1).getParagraphs().stream().map(ParagraphSimple::getTitle).toList());
    }

    /**
     * 把工作簿中指定名字的 sheet 改名：POI 写入侧禁止同名，须写出后改写 workbook.xml，
     * 模拟程序生成（而非 Excel 界面产出）的同名 sheet 文件。
     * 读取用 ZipFile（走中央目录）：POI 写出的本地头 size 字段不可靠，ZipInputStream 会拒读。
     */
    private static byte[] renameSheet(byte[] excel, String fromName, String toName) throws IOException {
        Path temp = Files.createTempFile("duplicate-sheet", ".xlsx");
        try {
            Files.write(temp, excel);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipFile source = new ZipFile(temp.toFile());
                 ZipOutputStream zip = new ZipOutputStream(out)) {
                Enumeration<? extends ZipEntry> entries = source.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    zip.putNextEntry(new ZipEntry(entry.getName()));
                    byte[] bytes = source.getInputStream(entry).readAllBytes();
                    if ("xl/workbook.xml".equals(entry.getName())) {
                        String xml = new String(bytes, StandardCharsets.UTF_8);
                        String renamed = xml.replace("name=\"" + fromName + "\"", "name=\"" + toName + "\"");
                        assertNotEquals(xml, renamed, "workbook.xml 中未找到待改名的 sheet，测试夹具失效");
                        bytes = renamed.getBytes(StandardCharsets.UTF_8);
                    }
                    zip.write(bytes);
                    zip.closeEntry();
                }
            }
            return out.toByteArray();
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void stubTransactionExecute() throws TransactionException {
        doAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null))
                .when(transactionTemplate).execute(any());
    }

    private static JSONObject knowledgeJson() {
        JSONObject json = new JSONObject();
        json.put("name", "知识库A");
        json.put("desc", "描述");
        return json;
    }

    private static byte[] singleSheetExcel(String sheetName, KnowledgeExcel... rows) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ExcelWriter excelWriter = EasyExcel.write(out, KnowledgeExcel.class).build()) {
            excelWriter.write(List.of(rows), EasyExcel.writerSheet(0, sheetName).build());
        }
        return out.toByteArray();
    }

    private static byte[] zipOf(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static KnowledgeExcel row(String title, String content, String problems) {
        KnowledgeExcel row = new KnowledgeExcel();
        row.setTitle(title);
        row.setContent(content);
        row.setProblems(problems);
        return row;
    }
}
