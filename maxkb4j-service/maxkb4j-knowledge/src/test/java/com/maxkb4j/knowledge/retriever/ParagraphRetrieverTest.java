package com.maxkb4j.knowledge.retriever;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.dto.DataSearchDTO;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ParagraphRetriever} 检索参数兜底与结果组装测试：
 * similarity/topNumber 缺省时回退默认值（避免拆箱 NPE），并按检索顺序回填得分。
 */
@ExtendWith(MockitoExtension.class)
class ParagraphRetrieverTest {

    private static final List<String> KNOWLEDGE_IDS = List.of("knowledge-1");

    @Mock
    private ParagraphMapper paragraphMapper;
    @Mock
    private DataRetriever dataRetriever;

    private ParagraphRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new ParagraphRetriever(paragraphMapper, dataRetriever);
    }

    @Test
    void paragraphSearch_missingSimilarityAndTopNumber_fallsBackToDefaults() {
        DataSearchDTO dto = new DataSearchDTO();
        dto.setQueryText("hello");

        retriever.paragraphSearch(KNOWLEDGE_IDS, dto);

        ArgumentCaptor<Integer> topNumber = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Float> similarity = ArgumentCaptor.forClass(Float.class);
        verify(dataRetriever).search(org.mockito.ArgumentMatchers.eq(KNOWLEDGE_IDS), any(),
                org.mockito.ArgumentMatchers.eq("hello"), topNumber.capture(),
                similarity.capture(), any());
        assertEquals(5, topNumber.getValue());
        assertEquals(0.5f, similarity.getValue());
    }

    @Test
    void paragraphSearch_explicitValues_passThroughUnchanged() {
        DataSearchDTO dto = new DataSearchDTO();
        dto.setQueryText("hello");
        dto.setSimilarity(0.8f);
        dto.setTopNumber(3);
        dto.setSearchMode("keywords");
        dto.setExcludeParagraphIds(List.of("p-excluded"));

        retriever.paragraphSearch(KNOWLEDGE_IDS, dto);

        ArgumentCaptor<Integer> topNumber = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Float> similarity = ArgumentCaptor.forClass(Float.class);
        verify(dataRetriever).search(org.mockito.ArgumentMatchers.eq(KNOWLEDGE_IDS),
                org.mockito.ArgumentMatchers.eq(List.of("p-excluded")),
                org.mockito.ArgumentMatchers.eq("hello"), topNumber.capture(),
                similarity.capture(), org.mockito.ArgumentMatchers.eq("keywords"));
        assertEquals(3, topNumber.getValue());
        assertEquals(0.8f, similarity.getValue());
    }

    @Test
    void paragraphSearch_byKnowledgeSetting_withNullFields_doesNotThrow() {
        KnowledgeSetting setting = new KnowledgeSetting();
        setting.setSearchMode("embedding");
        setting.setSimilarity(null);
        setting.setTopN(null);

        when(dataRetriever.search(anyList(), any(), anyString(), anyInt(), anyFloat(), anyString()))
                .thenReturn(List.of());

        retriever.paragraphSearch("question", KNOWLEDGE_IDS, List.of(), setting);

        ArgumentCaptor<Integer> topNumber = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Float> similarity = ArgumentCaptor.forClass(Float.class);
        verify(dataRetriever).search(org.mockito.ArgumentMatchers.eq(KNOWLEDGE_IDS), any(),
                org.mockito.ArgumentMatchers.eq("question"), topNumber.capture(),
                similarity.capture(), org.mockito.ArgumentMatchers.eq("embedding"));
        assertEquals(5, topNumber.getValue());
        assertEquals(0.5f, similarity.getValue());
    }

    @Test
    void paragraphSearch_emptyKnowledgeIds_skipsSearch() {
        List<ParagraphRagVO> result = retriever.paragraphSearch(List.of(), new DataSearchDTO());

        assertTrue(result.isEmpty());
        verifyNoInteractions(dataRetriever, paragraphMapper);
    }

    @Test
    void paragraphSearch_returnsParagraphsInSearchOrderWithScore() {
        ParagraphRagVO p1 = new ParagraphRagVO();
        p1.setId("p1");
        p1.setDocumentName(null);
        ParagraphRagVO p2 = new ParagraphRagVO();
        p2.setId("p2");
        when(dataRetriever.search(anyList(), any(), anyString(), anyInt(), anyFloat(), any()))
                .thenReturn(List.of(new TextChunkVO("p1", 0.9), new TextChunkVO("p2", 0.6)));
        when(paragraphMapper.retrievalParagraph(List.of("p1", "p2"))).thenReturn(List.of(p2, p1));

        List<ParagraphRagVO> result = retriever.paragraphSearch(KNOWLEDGE_IDS, buildDto());

        assertEquals(2, result.size());
        assertSame(p1, result.get(0));
        assertSame(p2, result.get(1));
        assertEquals(0.9, p1.getSimilarity());
        assertEquals("", p1.getDocumentName());
        assertEquals(0.6, p2.getSimilarity());
    }

    private DataSearchDTO buildDto() {
        DataSearchDTO dto = new DataSearchDTO();
        dto.setQueryText("hello");
        dto.setSimilarity(0.5f);
        dto.setTopNumber(5);
        return dto;
    }
}
