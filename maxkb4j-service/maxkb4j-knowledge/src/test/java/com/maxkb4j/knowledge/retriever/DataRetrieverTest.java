package com.maxkb4j.knowledge.retriever;

import com.maxkb4j.knowledge.retrieval.SearchMode;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DataRetriever} 检索模式路由测试：按 searchMode 选择对应 store、注入停用文档排除项。
 */
@ExtendWith(MockitoExtension.class)
class DataRetrieverTest {

    private static final List<String> KNOWLEDGE_IDS = List.of("knowledge-1");
    private static final List<TextChunkVO> EXPECTED = List.of(new TextChunkVO("p1", 0.9));

    @Mock
    private IDataStore vectorStore;
    @Mock
    private IDataStore fullTextStore;
    @Mock
    private IDataStore compositeStore;
    @Mock
    private IDocumentInternalService documentService;
    @Mock
    private SearchOrchestrator searchOrchestrator;

    private DataRetriever retriever;

    @BeforeEach
    void setUp() {
        // @Mock 字段由 MockitoExtension 在测试方法前注入，构造须推迟到 setUp
        retriever = new DataRetriever(
                vectorStore, fullTextStore, compositeStore, documentService, searchOrchestrator);
    }

    @Test
    void search_embeddingOrDefaultMode_usesVectorStore() {
        when(documentService.getNoActiveDocIds(KNOWLEDGE_IDS)).thenReturn(List.of());
        when(searchOrchestrator.search(any(), any())).thenReturn(EXPECTED);

        List<TextChunkVO> embedded = retriever.search(KNOWLEDGE_IDS, List.of(), "hello", 3, 0.5f, "embedding");
        List<TextChunkVO> defaulted = retriever.search(KNOWLEDGE_IDS, List.of(), "hello", 3, 0.5f, null);
        List<TextChunkVO> unknownMode = retriever.search(KNOWLEDGE_IDS, List.of(), "hello", 3, 0.5f, "whatever");

        assertSame(EXPECTED, embedded);
        assertSame(EXPECTED, defaulted);
        assertSame(EXPECTED, unknownMode);
        verify(searchOrchestrator, org.mockito.Mockito.times(3)).search(same(vectorStore), any());
    }

    @Test
    void search_fullTextMode_usesFullTextStore() {
        when(documentService.getNoActiveDocIds(KNOWLEDGE_IDS)).thenReturn(List.of("doc-9"));
        when(searchOrchestrator.search(any(), any())).thenReturn(EXPECTED);

        assertSame(EXPECTED, retriever.search(KNOWLEDGE_IDS, List.of(), "hello", 3, 0.5f, "keywords"));

        verify(searchOrchestrator).search(same(fullTextStore), any());

        SearchRequest sent = capturedRequest();
        assertEquals(SearchMode.FULL_TEXT, sent.getMode());
        assertEquals("hello", sent.getQuery());
        assertEquals(3, sent.getTopK());
        assertEquals(0.5, sent.getMinScore());
        assertEquals(List.of("doc-9"), sent.getExcludeDocumentIds());
    }

    @Test
    void search_hybridMode_usesCompositeStore() {
        when(documentService.getNoActiveDocIds(KNOWLEDGE_IDS)).thenReturn(List.of());
        when(searchOrchestrator.search(any(), any())).thenReturn(EXPECTED);

        assertSame(EXPECTED, retriever.search(KNOWLEDGE_IDS, List.of(), "hello", 3, 0.5f, "hybrid"));

        assertEquals(SearchMode.HYBRID, capturedRequest().getMode());
        verify(searchOrchestrator).search(same(compositeStore), any());
    }

    @Test
    void search_noInactiveDocs_leavesExcludeDocumentIdsUntouched() {
        when(documentService.getNoActiveDocIds(KNOWLEDGE_IDS)).thenReturn(List.of());
        when(searchOrchestrator.search(any(), any())).thenReturn(EXPECTED);

        retriever.search(KNOWLEDGE_IDS, List.of("p-excluded"), "hello", 3, 0.5f, "embedding");

        SearchRequest sent = capturedRequest();
        assertNull(sent.getExcludeDocumentIds());
        assertEquals(List.of("p-excluded"), sent.getExcludeParagraphIds());
        assertEquals(SearchMode.VECTOR, sent.getMode());
    }

    private SearchRequest capturedRequest() {
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(searchOrchestrator).search(any(IDataStore.class), captor.capture());
        return captor.getValue();
    }
}
