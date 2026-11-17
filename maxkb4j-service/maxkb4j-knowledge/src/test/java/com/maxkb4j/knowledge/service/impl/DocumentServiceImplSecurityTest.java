package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.maxkb4j.knowledge.dto.DatasetBatchHitHandlingDTO;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.mapper.DocumentMapper;
import com.maxkb4j.knowledge.mapper.DocumentTagMapper;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.mapper.ProblemParagraphMapper;
import com.maxkb4j.knowledge.service.IDocumentTagService;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
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
class DocumentServiceImplSecurityTest {

    private static final String KNOWLEDGE_ID = "knowledge-1";
    private static final String OWNED_DOC = "doc-owned";
    private static final String FOREIGN_DOC = "doc-foreign";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IProblemParagraphService problemParagraphService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IDocumentTagService documentTagService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IParagraphInternalService paragraphInternalService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private IDataStore compositeStore;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private ProblemParagraphMapper problemParagraphMapper;
    @Mock
    private DocumentTagMapper documentTagMapper;
    @Mock
    private ParagraphMapper paragraphMapper;

    private DocumentServiceImpl documentService;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境下 MyBatis-Plus 的 LambdaWrapper 解析列名依赖 TableInfo 缓存，需手动初始化
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        Stream.of(DocumentEntity.class, ProblemParagraphEntity.class, DocumentTagEntity.class, ParagraphEntity.class)
                .filter(clazz -> TableInfoHelper.getTableInfo(clazz) == null)
                .forEach(clazz -> TableInfoHelper.initTableInfo(assistant, clazz));
    }

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(
                paragraphInternalService, problemParagraphService, eventPublisher, compositeStore, documentTagService);
        ReflectionTestUtils.setField(documentService, "baseMapper", documentMapper);
    }

    @Test
    void deleteDocByIds_filtersForeignDocs() {
        DocumentEntity owned = new DocumentEntity();
        owned.setId(OWNED_DOC);
        when(documentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(owned));
        when(documentMapper.delete(any())).thenReturn(1);
        // 深桩 mock 解析不了链式包装器的 Children 泛型返回值，改为在 mock mapper 上构造真实链
        when(problemParagraphService.lambdaUpdate()).thenReturn(new LambdaUpdateChainWrapper<>(problemParagraphMapper));
        when(documentTagService.lambdaUpdate()).thenReturn(new LambdaUpdateChainWrapper<>(documentTagMapper));
        when(paragraphInternalService.lambdaUpdate()).thenReturn(new LambdaUpdateChainWrapper<>(paragraphMapper));

        boolean result = documentService.deleteDocByIds(KNOWLEDGE_ID, List.of(OWNED_DOC, FOREIGN_DOC));

        assertTrue(result);
        verify(compositeStore).deleteByDocumentIds(KNOWLEDGE_ID, List.of(OWNED_DOC));
    }

    @Test
    void deleteDocByIds_allForeignDocs_skipsRemoval() {
        when(documentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        boolean result = documentService.deleteDocByIds(KNOWLEDGE_ID, List.of(FOREIGN_DOC));

        assertFalse(result);
        verify(compositeStore, never()).deleteByDocumentIds(anyString(), anyList());
    }

    @Test
    void embedByDocIds_allForeignDocs_returnsFalse() {
        when(documentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        boolean result = documentService.embedByDocIds(KNOWLEDGE_ID, List.of(FOREIGN_DOC), List.of("0"));

        assertFalse(result);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void batchGenerateRelated_allForeignDocs_returnsFalse() {
        when(documentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        GenerateProblemDTO dto = new GenerateProblemDTO();
        dto.setDocumentIdList(List.of(OWNED_DOC, FOREIGN_DOC));

        boolean result = documentService.batchGenerateRelated(KNOWLEDGE_ID, dto);

        assertFalse(result);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void batchHitHandling_scopedToKnowledge() {
        // lambdaUpdate().update() 内部调用 baseMapper.update()
        when(documentMapper.update(any(), any())).thenReturn(1);

        DatasetBatchHitHandlingDTO dto = new DatasetBatchHitHandlingDTO();
        dto.setIdList(List.of(OWNED_DOC));
        dto.setHitHandlingMethod("directly_return");

        boolean result = documentService.batchHitHandling(KNOWLEDGE_ID, dto);

        assertTrue(result);
    }
}
