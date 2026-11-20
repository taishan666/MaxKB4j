package com.maxkb4j.knowledge.handler;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.dto.ParagraphSimple;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import com.maxkb4j.knowledge.service.DocumentWriteService;
import com.maxkb4j.knowledge.service.IKnowledgeInternalService;
import com.maxkb4j.model.service.IModelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
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
    private MultipartFile file;
    @InjectMocks
    private KnowledgeImportHandler knowledgeImportHandler;
    @Captor
    private ArgumentCaptor<List<DocumentSimple>> docsCaptor;

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

    private static KnowledgeExcel row(String title, String content, String problems) {
        KnowledgeExcel row = new KnowledgeExcel();
        row.setTitle(title);
        row.setContent(content);
        row.setProblems(problems);
        return row;
    }
}
