package com.maxkb4j.knowledge.service.impl;

import com.maxkb4j.common.exception.FileLimitExceededException;
import com.maxkb4j.knowledge.handler.DocumentHandler;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.knowledge.service.DocumentWriteService;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.service.IDocumentParseService;
import com.maxkb4j.knowledge.service.IDocumentSplitService;
import com.maxkb4j.knowledge.service.IDocumentWebService;
import com.maxkb4j.knowledge.vo.TextSegmentVO;
import com.maxkb4j.oss.service.IOssService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DocumentImportServiceSecurityTest {

    @Mock
    private IDocumentInternalService documentService;
    @Mock
    private IDocumentParseService documentParseService;
    @Mock
    private IDocumentSplitService documentSpiltService;
    @Mock
    private IOssService ossService;
    @Mock
    private IDocumentWebService documentWebService;
    @Mock
    private DocumentWriteService documentWriteService;
    @Mock
    private DocumentHandler documentHandler;
    @Mock
    private KnowledgeMapper knowledgeMapper;

    @InjectMocks
    private DocumentImportService documentImportService;

    @Test
    void splitReadsValidZipEntry() throws IOException {
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = zipFile(zip("a.txt", content));

        List<TextSegmentVO> result = documentImportService.split(
                "knowledge-1", new MockMultipartFile[]{file}, null, null, null);

        assertEquals(1, result.size());
        assertEquals("a.txt", result.get(0).getName());
    }

    @Test
    void splitRejectsTooManyZipEntries() throws IOException {
        byte[] zipBytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int i = 0; i <= DocumentImportService.ZIP_ENTRY_COUNT_LIMIT; i++) {
                zip.putNextEntry(new ZipEntry("file-" + i + ".txt"));
                zip.closeEntry();
            }
            zip.finish();
            zipBytes = output.toByteArray();
        }

        assertThrows(FileLimitExceededException.class, () -> documentImportService.split(
                "knowledge-1", new MockMultipartFile[]{zipFile(zipBytes)}, null, null, null));
        verifyNoInteractions(ossService, documentParseService);
    }

    @Test
    void copyBoundedAcceptsDataAtLimits() throws IOException {
        byte[] content = new byte[16];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) i;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long size = DocumentImportService.copyBounded(
                new ByteArrayInputStream(content), output, content.length, content.length);

        assertEquals(content.length, size);
        assertArrayEquals(content, output.toByteArray());
    }

    @Test
    void copyBoundedRejectsOversizedEntryBeforeWritingExcess() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThrows(FileLimitExceededException.class, () -> DocumentImportService.copyBounded(
                new ByteArrayInputStream(new byte[17]), output, 16, 100));
        assertEquals(0, output.size());
    }

    @Test
    void copyBoundedRejectsExcessTotalSize() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThrows(FileLimitExceededException.class, () -> DocumentImportService.copyBounded(
                new ByteArrayInputStream(new byte[11]), output, 100, 10));
        assertEquals(0, output.size());
    }

    private static MockMultipartFile zipFile(byte[] content) {
        return new MockMultipartFile("file", "documents.zip", "application/zip", content);
    }

    private static byte[] zip(String name, byte[] content) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content);
            zip.closeEntry();
            zip.finish();
            return output.toByteArray();
        }
    }
}
