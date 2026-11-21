package com.maxkb4j.knowledge.retriever;

import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.maxkb4j.knowledge.consts.SourceType.PARAGRAPH;
import static com.maxkb4j.knowledge.consts.SourceType.PROBLEM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link SearchOrchestrator} 检索编排逻辑测试：短路条件、双路召回、问题映射段落、去重排序截断。
 */
@ExtendWith(MockitoExtension.class)
class SearchOrchestratorTest {

    private static final List<String> KNOWLEDGE_IDS = List.of("knowledge-1");

    @Mock
    private IParagraphInternalService paragraphService;
    @Mock
    private IProblemParagraphService problemParagraphService;
    @Mock
    private IDataStore store;

    private SearchOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // @Mock 字段由 MockitoExtension 在测试方法前注入，构造须推迟到 setUp
        orchestrator = new SearchOrchestrator(paragraphService, problemParagraphService);
    }

    @Test
    void search_shortCircuitsOnBlankQueryOrEmptyKnowledgeIds() {
        assertEquals(List.of(), orchestrator.search(store, request(" ", 5)));
        assertEquals(List.of(), orchestrator.search(store, request(null, 5)));
        SearchRequest noKnowledge = request("hello", 5);
        noKnowledge.setKnowledgeIds(List.of());
        assertEquals(List.of(), orchestrator.search(store, noKnowledge));
        assertEquals(List.of(), orchestrator.search(store, request("hello", 0)));

        verifyNoInteractions(store, paragraphService, problemParagraphService);
    }

    @Test
    void search_mergesParagraphAndProblemRoutes_andMapsProblemsToParagraphs() {
        SearchRequest request = request("hello", 2);
        request.setExcludeParagraphIds(List.of("excluded"));
        when(paragraphService.getNoActiveParagraphIds(KNOWLEDGE_IDS, null)).thenReturn(List.of("inactive"));
        when(store.searchBySource(any(), anyInt())).thenAnswer(invocation -> {
            SearchRequest req = invocation.getArgument(0);
            if (PARAGRAPH == (Integer) invocation.getArgument(1)) {
                return List.of(new TextChunkVO("p1", 0.9), new TextChunkVO("p2", 0.8));
            }
            return List.of(new TextChunkVO("prob1", 0.7));
        });
        when(problemParagraphService.getActivePPbyProblemIds(List.of("prob1")))
                .thenReturn(List.of(pp("prob1", "p2")));

        List<TextChunkVO> results = orchestrator.search(store, request);

        // 命中的问题 prob1 映射回段落 p2，与段落路结果合并去重后按分数降序
        assertEquals(List.of("p1", "p2"), results.stream().map(TextChunkVO::getSourceId).toList());
        assertEquals(0.9, results.get(0).getScore());
        assertEquals(0.8, results.get(1).getScore());

        // 排除段落 = 非激活段落 + 调用方显式排除；topK 按召回放大系数放大后传给 store
        @SuppressWarnings("unchecked")
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store, org.mockito.Mockito.times(2)).searchBySource(requestCaptor.capture(), anyInt());
        SearchRequest sent = requestCaptor.getValue();
        assertEquals(List.of("inactive", "excluded"), sent.getExcludeParagraphIds());
        assertEquals(6, sent.getTopK());
    }

    @Test
    void search_sameProblemAcrossRoutes_keepsHighestScore() {
        SearchRequest request = request("hello", 5);
        when(store.searchBySource(any(), anyInt())).thenAnswer(invocation -> {
            if (PARAGRAPH == (Integer) invocation.getArgument(1)) {
                return List.of(new TextChunkVO("p1", 0.9));
            }
            // 同一问题在问题路命中两次，取最高分 0.8
            return List.of(new TextChunkVO("prob1", 0.8), new TextChunkVO("prob1", 0.6));
        });
        when(problemParagraphService.getActivePPbyProblemIds(anyList()))
                .thenReturn(List.of(pp("prob1", "p1")));

        List<TextChunkVO> results = orchestrator.search(store, request);

        assertEquals(1, results.size());
        assertEquals("p1", results.get(0).getSourceId());
        assertEquals(0.9, results.get(0).getScore());
    }

    @Test
    void search_dedupesAndTruncatesToTopK() {
        SearchRequest request = request("hello", 2);
        when(paragraphService.getNoActiveParagraphIds(any(), any())).thenReturn(List.of());
        when(store.searchBySource(any(), anyInt())).thenAnswer(invocation -> {
            if (PARAGRAPH == (Integer) invocation.getArgument(1)) {
                return List.of(new TextChunkVO("p1", 0.9), new TextChunkVO("p2", 0.7), new TextChunkVO("p3", 0.6));
            }
            return List.of();
        });

        List<TextChunkVO> results = orchestrator.search(store, request);

        assertEquals(2, results.size());
        assertEquals(List.of("p1", "p2"), results.stream().map(TextChunkVO::getSourceId).toList());
    }

    @Test
    void search_multipleProblemHitsOnSameParagraph_aggTotalScoreBreaksTie() {
        SearchRequest request = request("hello", 5);
        when(store.searchBySource(any(), anyInt())).thenAnswer(invocation -> {
            if (PARAGRAPH == (Integer) invocation.getArgument(1)) {
                return List.of();
            }
            // p1 被两个问题命中（各 0.5），p2 只被一个问题命中（0.5）——同分时累计分高者靠前
            return List.of(new TextChunkVO("prob1", 0.5), new TextChunkVO("prob2", 0.5), new TextChunkVO("prob3", 0.5));
        });
        when(problemParagraphService.getActivePPbyProblemIds(anyList()))
                .thenReturn(List.of(pp("prob1", "p1"), pp("prob2", "p1"), pp("prob3", "p2")));

        List<TextChunkVO> results = orchestrator.search(store, request);

        assertEquals(2, results.size());
        assertEquals("p1", results.get(0).getSourceId());
        assertEquals(0.5, results.get(0).getScore());
        assertTrue(results.get(1).getScore() == 0.5);
    }

    private static SearchRequest request(String query, int topK) {
        SearchRequest request = new SearchRequest();
        request.setKnowledgeIds(KNOWLEDGE_IDS);
        request.setQuery(query);
        request.setTopK(topK);
        return request;
    }

    private static ProblemParagraphEntity pp(String problemId, String paragraphId) {
        ProblemParagraphEntity entity = new ProblemParagraphEntity();
        entity.setProblemId(problemId);
        entity.setParagraphId(paragraphId);
        return entity;
    }
}
