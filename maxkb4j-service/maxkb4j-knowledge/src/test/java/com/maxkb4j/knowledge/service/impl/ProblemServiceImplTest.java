package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.maxkb4j.core.assistant.ProblemGenerateAssistant;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.mapper.ProblemMapper;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.store.IDataStore;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProblemServiceImpl#generateRelated} 回归测试：LLM 重复输出同一问题时的去重行为。
 *
 * <p>修复前：LLM 对同一问题输出两次时，同一 problemId 的实体两次进入 newProblems，
 * {@code saveBatch} 主键冲突导致整段落的问题生成失败。</p>
 *
 * <p>LLM 调用经 {@code mockStatic(AiServiceFactory.class)} 打桩，返回指定的伪输出；
 * 落库通过 {@code @Spy} 拦截 {@code saveBatchProblems}，用入参断言去重结果，
 * 不依赖真实 MyBatis 批量会话。</p>
 */
@ExtendWith(MockitoExtension.class)
class ProblemServiceImplTest {

    private static final String KNOWLEDGE_ID = "knowledge-1";

    @Mock
    private IProblemParagraphService problemParagraphService;
    @Mock
    private IDataStore compositeStore;
    @Mock
    private KnowledgeModelService knowledgeModelService;
    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private ChatModel chatModel;
    @Mock
    private EmbeddingModel embeddingModel;

    @Spy
    @InjectMocks
    private ProblemServiceImpl problemService;

    @BeforeEach
    void setUp() {
        // reIndexBatch -> createIndexBatch -> listByIds 需要 baseMapper，纯 Mockito 环境下手动注入
        ReflectionTestUtils.setField(problemService, "baseMapper", problemMapper);
    }

    @Test
    void generateRelated_repeatedLlmQuestion_savesEachProblemIdOnlyOnce() {
        // 模拟 LLM 对同一问题重复输出两次（修复前：两个相同 id 的实体入批 → 主键冲突）
        try (MockedStatic<AiServiceFactory> aiFactory = mockStatic(AiServiceFactory.class)) {
            stubAssistant(aiFactory, List.of("如何退货？", "如何开发票？", "如何退货？"));
            stubAssociationQuery(List.of());

            doReturn(true).when(problemService).saveBatchProblems(anyList());
            when(knowledgeModelService.getEmbeddingModel(KNOWLEDGE_ID)).thenReturn(embeddingModel);
            // reIndex 批量查库返回空列表，后续向量写入自然跳过（lenient：不依赖 MP 版本内部委托方法）
            lenient().doReturn(List.of()).when(problemMapper).selectByIds(anyCollection());

            problemService.generateRelated(chatModel, embeddingModel, KNOWLEDGE_ID, "doc-1",
                    paragraph("para-1"), List.of(), 3);

            // 回归断言：重复输出的问题只入库一次，且实体 id 互不重复
            ArgumentCaptor<List<ProblemEntity>> problemsCaptor = listCaptor();
            verify(problemService).saveBatchProblems(problemsCaptor.capture());
            List<ProblemEntity> saved = problemsCaptor.getValue();
            assertEquals(2, saved.size(), "重复输出的问题不应产生两条实体");
            assertEquals(2, saved.stream().map(ProblemEntity::getId).distinct().count(),
                    "同一 problemId 不得重复入批（saveBatch 主键冲突根因）");
            assertEquals(Set.of("如何退货？", "如何开发票？"),
                    saved.stream().map(ProblemEntity::getContent).collect(Collectors.toSet()));

            // 关联表同样只按去重后的问题建行
            ArgumentCaptor<List<ProblemParagraphEntity>> assocCaptor = listCaptor();
            verify(problemParagraphService).saveBatch(assocCaptor.capture());
            assertEquals(2, assocCaptor.getValue().size(), "重复输出不应建立重复关联");
        }
    }

    @Test
    void generateRelated_repeatedExistingQuestion_noSaveNoDuplicatedAssociation() {
        ProblemEntity existing = new ProblemEntity();
        existing.setId("p-existing");
        existing.setContent("如何退货？");

        try (MockedStatic<AiServiceFactory> aiFactory = mockStatic(AiServiceFactory.class)) {
            stubAssistant(aiFactory, List.of("如何退货？", "如何退货？"));
            // 该问题与当前段落已存在关联
            stubAssociationQuery(List.of("p-existing"));

            problemService.generateRelated(chatModel, embeddingModel, KNOWLEDGE_ID, "doc-1",
                    paragraph("para-1"), List.of(existing), 2);

            // 全部命中已有问题且关联已存在：不落库新问题、不重复建关联、不重建向量
            verify(problemService, never()).saveBatchProblems(anyList());
            verify(problemParagraphService, never()).saveBatch(anyCollection());
            verify(knowledgeModelService, never()).getEmbeddingModel(anyString());
        }
    }

    /**
     * 打桩 LLM 工厂与助手：{@code AiServiceFactory.builder(...)} 返回链式 mock，
     * {@code generate} 返回指定的伪输出
     */
    private void stubAssistant(MockedStatic<AiServiceFactory> aiFactory, List<String> questions) {
        ProblemGenerateAssistant assistant = mock(ProblemGenerateAssistant.class);
        @SuppressWarnings("unchecked")
        AiServices<ProblemGenerateAssistant> aiServices = mock(AiServices.class);
        when(aiServices.chatModel(any(ChatModel.class))).thenReturn(aiServices);
        when(aiServices.build()).thenReturn(assistant);
        aiFactory.when(() -> AiServiceFactory.builder(ProblemGenerateAssistant.class)).thenReturn(aiServices);
        Result<List<String>> response = Result.<List<String>>builder().content(questions).build();
        when(assistant.generate(anyInt(), anyString())).thenReturn(response);
    }

    /**
     * 打桩问题-段落关联查询链（{@link ProblemServiceImpl#findExistingAssociations}）
     */
    private void stubAssociationQuery(List<String> existingProblemIds) {
        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ProblemParagraphEntity> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(problemParagraphService).lambdaQuery();
        when(chain.select(any(SFunction.class))).thenReturn(chain);
        when(chain.in(any(SFunction.class), anyCollection())).thenReturn(chain);
        when(chain.eq(any(SFunction.class), any())).thenReturn(chain);
        when(chain.list()).thenReturn(existingProblemIds.stream().map(id -> {
            ProblemParagraphEntity row = new ProblemParagraphEntity();
            row.setProblemId(id);
            return row;
        }).collect(Collectors.toList()));
    }

    private ParagraphEntity paragraph(String id) {
        ParagraphEntity paragraph = new ParagraphEntity();
        paragraph.setId(id);
        paragraph.setContent("段落内容");
        return paragraph;
    }

    @SuppressWarnings("unchecked")
    private <E> ArgumentCaptor<List<E>> listCaptor() {
        return ArgumentCaptor.forClass((Class<List<E>>) (Class<?>) List.class);
    }
}
