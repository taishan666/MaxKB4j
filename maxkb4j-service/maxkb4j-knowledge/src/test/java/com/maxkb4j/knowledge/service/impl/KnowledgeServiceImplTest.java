package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.knowledge.dto.KnowledgeSimple;
import com.maxkb4j.knowledge.dto.WebKnowledgeDTO;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.entity.KnowledgeVersionEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.event.CreateWebDocsEvent;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.mapper.ProblemMapper;
import com.maxkb4j.knowledge.mapper.ProblemParagraphMapper;
import com.maxkb4j.knowledge.service.IKnowledgeActionInternalService;
import com.maxkb4j.knowledge.service.IKnowledgeVersionService;
import com.maxkb4j.knowledge.service.ITagService;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.system.dto.TargetResource;
import com.maxkb4j.system.service.IResourceMappingService;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeServiceImpl} 核心逻辑测试：简单信息查询、删除编排、创建默认值。
 *
 * <p>MyBatis-Plus ServiceImpl 在纯 Mockito 环境下需手动初始化 TableInfo 并注入 baseMapper，
 * {@code removeByIds/save} 等 batch 操作通过 {@code @Spy} 打桩拦截。</p>
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceImplTest {

    private static final String KNOWLEDGE_ID = "knowledge-1";

    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private ParagraphMapper paragraphMapper;
    @Mock
    private ProblemParagraphMapper problemParagraphMapper;
    @Mock
    private IDocumentInternalService documentService;
    @Mock
    private IUserResourcePermissionService userResourcePermissionService;
    @Mock
    private DataPermissionSupport dataPermissionSupport;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private IDataStore compositeStore;
    @Mock
    private IKnowledgeActionInternalService knowledgeActionService;
    @Mock
    private IKnowledgeVersionService knowledgeVersionService;
    @Mock
    private IResourceMappingService resourceMappingService;
    @Mock
    private ITagService tagService;
    @Mock
    private UserContext userContext;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private KnowledgeMapper knowledgeMapper;

    @Spy
    @InjectMocks
    private KnowledgeServiceImpl knowledgeService;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境下 MyBatis-Plus 的 LambdaWrapper 解析列名依赖 TableInfo 缓存，需手动初始化
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        if (TableInfoHelper.getTableInfo(KnowledgeEntity.class) == null) {
            TableInfoHelper.initTableInfo(assistant, KnowledgeEntity.class);
        }
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(knowledgeService, "baseMapper", knowledgeMapper);
        ReflectionTestUtils.setField(knowledgeService, "transactionTemplate",
                new TransactionTemplate(transactionManager));
    }

    @Test
    void listSimpleKnowledgeByIds_copiesIdNameDescAndIntegerType() {
        KnowledgeEntity entity = new KnowledgeEntity();
        entity.setId(KNOWLEDGE_ID);
        entity.setName("产品知识库");
        entity.setDesc("说明");
        entity.setType(1);
        entity.setUserId("user-1");
        entity.setEmbeddingModelId("model-1");
        // MP 泛型链式包装器在纯 Mockito 下无法依托真实 mapper 代理运行，改为显式打桩链
        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<KnowledgeEntity> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(knowledgeService).lambdaQuery();
        when(chain.select(any(SFunction.class), any(SFunction.class), any(SFunction.class), any(SFunction.class)))
                .thenReturn(chain);
        when(chain.in(any(SFunction.class), anyCollection())).thenReturn(chain);
        when(chain.orderByDesc(any(SFunction.class))).thenReturn(chain);
        when(chain.list()).thenReturn(List.of(entity));

        List<KnowledgeSimple> simples = knowledgeService.listSimpleKnowledgeByIds(List.of(KNOWLEDGE_ID));

        assertEquals(1, simples.size());
        KnowledgeSimple simple = simples.get(0);
        assertEquals(KNOWLEDGE_ID, simple.getId());
        assertEquals("产品知识库", simple.getName());
        assertEquals("说明", simple.getDesc());
        // 回归 type String -> Integer 调整：简单查询按 Integer 原样复制
        assertEquals(Integer.valueOf(1), simple.getType());
    }

    @Test
    void deleteById_removesRelationalRowsAndVectorStoreByChunk() {
        doReturn(true).when(knowledgeService).removeByIds(anyCollection());
        LambdaUpdateChainWrapper<KnowledgeVersionEntity> versionChain = stubLambdaUpdateChain(knowledgeVersionService);
        LambdaUpdateChainWrapper<KnowledgeActionEntity> actionChain = stubLambdaUpdateChain(knowledgeActionService);
        LambdaUpdateChainWrapper<TagEntity> tagChain = stubLambdaUpdateChain(tagService);

        assertTrue(knowledgeService.deleteById(KNOWLEDGE_ID));

        // 关系型数据先删
        verify(problemParagraphMapper).delete(any());
        verify(problemMapper).delete(any());
        verify(paragraphMapper).delete(any());
        verify(documentService).deleteByKnowledgeIds(List.of(KNOWLEDGE_ID));
        verify(userResourcePermissionService).remove(AuthTargetType.KNOWLEDGE, List.of(KNOWLEDGE_ID));
        verify(resourceMappingService).deleteBySourceIds(ResourceType.KNOWLEDGE, List.of(KNOWLEDGE_ID));
        verify(versionChain).in(any(), anyCollection());
        verify(versionChain).remove();
        verify(actionChain).in(any(), anyCollection());
        verify(actionChain).remove();
        verify(tagChain).in(any(), anyCollection());
        verify(tagChain).remove();
        // 关系型删除后清理向量/全文库
        verify(compositeStore).deleteByKnowledgeIds(List.of(KNOWLEDGE_ID));
    }

    @Test
    void deleteById_vectorStoreFailure_doesNotBreakRelationalCleanup() {
        doReturn(true).when(knowledgeService).removeByIds(anyCollection());
        stubLambdaUpdateChain(knowledgeVersionService);
        stubLambdaUpdateChain(knowledgeActionService);
        stubLambdaUpdateChain(tagService);
        doThrow(new RuntimeException("pg down")).when(compositeStore).deleteByKnowledgeIds(anyList());

        // 向量库失败只记日志，关系型行已删除，删除结果不受影响
        assertTrue(knowledgeService.deleteById(KNOWLEDGE_ID));
        verify(compositeStore).deleteByKnowledgeIds(List.of(KNOWLEDGE_ID));
    }

    @Test
    void createKnowledge_fillsOwnerAndDefaults_grantsPermissionAndSavesMapping() {
        when(userContext.getUserId()).thenReturn("user-1");
        doReturn(true).when(knowledgeService).save(any(KnowledgeEntity.class));

        KnowledgeEntity knowledge = new KnowledgeEntity();
        knowledge.setName("新知识库");
        knowledge.setEmbeddingModelId("model-1");

        KnowledgeEntity created = knowledgeService.createKnowledge(knowledge);

        assertEquals("user-1", created.getUserId());
        assertEquals(0, created.getMeta().size());
        assertEquals(0, created.getWorkFlow().size());
        verify(userResourcePermissionService).ownerSave(AuthTargetType.KNOWLEDGE, created.getId(), "user-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TargetResource>> targetsCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceMappingService).relation(eq(ResourceType.KNOWLEDGE), eq(created.getId()),
                targetsCaptor.capture());
        // embedding 模型（非空）进入资源映射；toolIds 为空
        assertEquals(1, targetsCaptor.getValue().size());
        assertEquals("model-1", targetsCaptor.getValue().get(0).getTargetId());
        assertEquals(ResourceType.MODEL, targetsCaptor.getValue().get(0).getTargetType());
    }

    @Test
    void createKnowledgeWeb_publishesEventAfterCreation() {
        when(userContext.getUserId()).thenReturn("user-1");
        doReturn(true).when(knowledgeService).save(any(KnowledgeEntity.class));

        com.maxkb4j.knowledge.dto.WebKnowledgeDTO dto = new com.maxkb4j.knowledge.dto.WebKnowledgeDTO();
        dto.setSourceUrl("https://example.com/docs");
        dto.setSelector(".content");

        KnowledgeEntity created = knowledgeService.createKnowledgeWeb(dto);

        verify(knowledgeService).save(created);
        verify(eventPublisher).publishEvent(any(CreateWebDocsEvent.class));
    }

    @Test
    void getKnowledgeById_missing_returnsNull() {
        when(knowledgeMapper.selectById("missing")).thenReturn(null);
        assertNull(knowledgeService.getKnowledgeById("missing"));
    }

    /**
     * 为 ServiceImpl 的 {@code lambdaUpdate().in(...).remove()} 清理链显式打桩
     * （泛型链式方法无法被 RETURNS_DEEP_STUBS 解析）。
     */
    private <T> LambdaUpdateChainWrapper<T> stubLambdaUpdateChain(IService<T> service) {
        @SuppressWarnings("unchecked")
        LambdaUpdateChainWrapper<T> chain = mock(LambdaUpdateChainWrapper.class);
        when(service.lambdaUpdate()).thenReturn(chain);
        when(chain.in(any(), anyCollection())).thenReturn(chain);
        when(chain.remove()).thenReturn(true);
        return chain;
    }
}
