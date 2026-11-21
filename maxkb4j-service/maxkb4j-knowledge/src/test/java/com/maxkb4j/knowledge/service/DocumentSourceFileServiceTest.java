package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.oss.service.IOssService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DocumentSourceFileService#replace} 的 meta 合并与 storeFile 空返回防护测试。
 */
@ExtendWith(MockitoExtension.class)
class DocumentSourceFileServiceTest {

    private static final String KNOWLEDGE_ID = "knowledge-1";
    private static final String DOC_ID = "doc-1";

    @Mock
    private IDocumentInternalService documentService;
    @Mock
    private IOssService ossService;
    @Mock
    private HttpServletResponse response;
    @InjectMocks
    private DocumentSourceFileService service;

    private DocumentEntity docWithMeta(JSONObject meta) {
        DocumentEntity doc = new DocumentEntity();
        doc.setId(DOC_ID);
        doc.setKnowledgeId(KNOWLEDGE_ID);
        doc.setMeta(meta);
        return doc;
    }

    @Test
    void replaceShouldFailFastWhenStoreFileReturnsNull() throws IOException {
        when(documentService.getByKnowledgeIdAndId(KNOWLEDGE_ID, DOC_ID))
                .thenReturn(docWithMeta(new JSONObject()));
        when(ossService.storeFile(any(), anyString(), anyString())).thenReturn(null);
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});

        boolean result = service.replace(KNOWLEDGE_ID, DOC_ID, file);

        assertFalse(result);
        verify(documentService, never()).updateById(any(DocumentEntity.class));
    }

    @Test
    void replaceShouldMergeMetaAndKeepWebDocSourceUrlAndSelector() throws IOException {
        JSONObject webMeta = new JSONObject();
        webMeta.put("sourceUrl", "https://example.com/page");
        webMeta.put("selector", "body");
        DocumentEntity doc = docWithMeta(webMeta);
        when(documentService.getByKnowledgeIdAndId(KNOWLEDGE_ID, DOC_ID)).thenReturn(doc);
        when(ossService.storeFile(any(), anyString(), anyString())).thenReturn("oss-file-id");
        when(documentService.updateById(doc)).thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});

        boolean result = service.replace(KNOWLEDGE_ID, DOC_ID, file);

        assertTrue(result);
        JSONObject meta = doc.getMeta();
        assertEquals("oss-file-id", meta.getString("sourceFileId"));
        assertEquals(true, meta.getBoolean("allow_download"));
        // Web 文档的同步字段必须保留，否则 syncWebDoc 永久失效
        assertEquals("https://example.com/page", meta.getString("sourceUrl"));
        assertEquals("body", meta.getString("selector"));
    }

    @Test
    void replaceShouldInitMetaWhenDocHasNone() throws IOException {
        DocumentEntity doc = docWithMeta(null);
        when(documentService.getByKnowledgeIdAndId(KNOWLEDGE_ID, DOC_ID)).thenReturn(doc);
        when(ossService.storeFile(any(), anyString(), anyString())).thenReturn("oss-file-id");
        when(documentService.updateById(doc)).thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});

        boolean result = service.replace(KNOWLEDGE_ID, DOC_ID, file);

        assertTrue(result);
        JSONObject meta = doc.getMeta();
        assertEquals("oss-file-id", meta.getString("sourceFileId"));
        assertEquals(true, meta.getBoolean("allow_download"));
    }

    @Test
    void downloadShouldReturnFalseWhenDocMissing() throws IOException {
        when(documentService.getByKnowledgeIdAndId(KNOWLEDGE_ID, DOC_ID)).thenReturn(null);

        assertFalse(service.download(KNOWLEDGE_ID, DOC_ID, response));
    }
}
