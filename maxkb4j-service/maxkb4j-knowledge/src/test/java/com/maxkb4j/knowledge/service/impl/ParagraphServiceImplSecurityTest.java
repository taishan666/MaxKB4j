package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.dto.ParagraphAddDTO;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.mapper.DocumentMapper;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.service.IProblemService;
import com.maxkb4j.knowledge.store.IDataStore;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParagraphServiceImplSecurityTest {

    private static final String KNOWLEDGE_ID = "knowledge-1";
    private static final String DOC_ID = "doc-1";
    private static final String OWNED_PARAGRAPH = "p-owned";
    private static final String FOREIGN_PARAGRAPH = "p-foreign";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IProblemService problemService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IProblemParagraphService problemParagraphService;
    @Mock
    private IDataStore compositeStore;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ParagraphMapper paragraphMapper;

    private ParagraphServiceImpl paragraphService;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境下 MyBatis-Plus 的 LambdaWrapper 解析列名依赖 TableInfo 缓存，需手动初始化
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        Stream.of(ParagraphEntity.class, DocumentEntity.class)
                .filter(clazz -> TableInfoHelper.getTableInfo(clazz) == null)
                .forEach(clazz -> TableInfoHelper.initTableInfo(assistant, clazz));
    }

    @BeforeEach
    void setUp() {
        paragraphService = new ParagraphServiceImpl(
                problemService, problemParagraphService, compositeStore, documentMapper, eventPublisher);
        ReflectionTestUtils.setField(paragraphService, "baseMapper", paragraphMapper);
    }

    @Test
    void deleteBatchByIds_allForeign_skipsRemoval() {
        when(paragraphMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        boolean result = paragraphService.deleteBatchByIds(KNOWLEDGE_ID, DOC_ID, List.of(FOREIGN_PARAGRAPH));

        assertFalse(result);
        verify(compositeStore, never()).deleteByParagraphIds(anyString(), anyList());
    }

    @Test
    void updateParagraphById_foreignParagraph_doesNotPublishEvent() {
        ParagraphEntity entity = new ParagraphEntity();
        entity.setId(FOREIGN_PARAGRAPH);
        entity.setContent("new content");

        when(paragraphMapper.update(any(), any())).thenReturn(0);

        paragraphService.updateParagraphById(KNOWLEDGE_ID, DOC_ID, entity);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void saveParagraphAndProblem_withoutDocument_returnsFalse() {
        when(documentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ParagraphAddDTO dto = new ParagraphAddDTO();
        dto.setTitle("title");
        dto.setContent("content");

        boolean result = paragraphService.saveParagraphAndProblem(KNOWLEDGE_ID, DOC_ID, dto);

        assertFalse(result);
    }

    @Test
    void batchGenerateRelated_allForeign_returnsFalse() {
        when(paragraphMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        GenerateProblemDTO dto = new GenerateProblemDTO();
        dto.setParagraphIdList(List.of(FOREIGN_PARAGRAPH));

        boolean result = paragraphService.batchGenerateRelated(KNOWLEDGE_ID, DOC_ID, dto);

        assertFalse(result);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void getProblemsByParagraphId_foreign_returnsEmpty() {
        when(paragraphMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        List<?> problems = paragraphService.getProblemsByParagraphId(KNOWLEDGE_ID, DOC_ID, FOREIGN_PARAGRAPH);

        assertTrue(problems.isEmpty());
    }
}
