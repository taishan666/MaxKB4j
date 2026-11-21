package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.dto.ParagraphSimple;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.event.DocumentIndexEvent;
import com.maxkb4j.knowledge.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link DocumentWriteService#batchCreateDocs} 落库与事件发布逻辑测试。
 *
 * <p>被测类继承 MyBatis-Plus ServiceImpl，{@code saveBatch} 需要 SqlSession 环境，
 * 纯 Mockito 下通过 {@code @Spy @InjectMocks} 将其拦截打桩，只校验入参组装逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
class DocumentWriteServiceTest {

    private static final String KNOWLEDGE_ID = "knowledge-1";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IProblemService problemService;
    @Mock
    private IParagraphInternalService paragraphService;
    @Mock
    private IProblemParagraphService problemParagraphService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private DocumentMapper documentMapper;

    @Spy
    @InjectMocks
    private DocumentWriteService documentWriteService;

    @Captor
    private ArgumentCaptor<List<DocumentEntity>> docsCaptor;
    @Captor
    private ArgumentCaptor<List<ParagraphEntity>> paragraphsCaptor;
    @Captor
    private ArgumentCaptor<List<ProblemEntity>> problemsCaptor;
    @Captor
    private ArgumentCaptor<List<ProblemParagraphEntity>> problemParagraphsCaptor;
    @Captor
    private ArgumentCaptor<DocumentIndexEvent> eventCaptor;

    @Test
    void batchCreateDocs_emptyList_returnsTrueWithoutAnyWrite() {
        assertTrue(documentWriteService.batchCreateDocs(KNOWLEDGE_ID, KnowledgeType.BASE, List.of()));

        verifyNoInteractions(paragraphService, problemService, problemParagraphService, eventPublisher);
        verify(documentWriteService, never()).saveBatch(anyList());
    }

    @Test
    void batchCreateDocs_createsDocsParagraphsProblems_andPublishesIndexEvent() {
        stubExistingProblems(List.of());
        stubProblemLookup();
        stubParagraphCreation();
        doReturn(true).when(documentWriteService).saveBatch(anyList());

        ParagraphSimple first = paragraph("标题一", "内容一", List.of(" 问题A ", "  "));
        ParagraphSimple second = paragraph("标题二", "内容二", List.of("问题A"));
        DocumentSimple doc = new DocumentSimple("文档一", null);
        doc.setParagraphs(List.of(first, second));
        doc.setSourceFileId("file-1");

        boolean result = documentWriteService.batchCreateDocs(KNOWLEDGE_ID, KnowledgeType.BASE, List.of(doc));

        assertTrue(result);

        // 文档：字数统计 = 各段落内容长度；meta 写入下载开关与源文件 ID
        verify(documentWriteService).saveBatch(docsCaptor.capture());
        DocumentEntity savedDoc = docsCaptor.getValue().get(0);
        assertEquals(KNOWLEDGE_ID, savedDoc.getKnowledgeId());
        assertEquals("文档一", savedDoc.getName());
        assertEquals(Integer.valueOf(KnowledgeType.BASE), savedDoc.getType());
        assertEquals(6, savedDoc.getCharLength());
        assertEquals("file-1", savedDoc.getMeta().getString("sourceFileId"));
        assertEquals(Boolean.TRUE, savedDoc.getMeta().getBoolean("allow_download"));

        // 段落：两个段落按顺序落库
        verify(paragraphService).saveBatch(paragraphsCaptor.capture());
        assertEquals(List.of("p-1", "p-2"),
                paragraphsCaptor.getValue().stream().map(ParagraphEntity::getId).toList());

        // 问题：跨段落重复的"问题A"只新建一次；空白的提问被跳过
        verify(problemService).saveBatch(problemsCaptor.capture());
        List<ProblemEntity> savedProblems = problemsCaptor.getValue();
        assertEquals(1, savedProblems.size());
        assertEquals("问题A", savedProblems.get(0).getContent());

        // 问题-段落关联：两个段落各自关联同一个问题
        verify(problemParagraphService).saveBatch(problemParagraphsCaptor.capture());
        List<ProblemParagraphEntity> links = problemParagraphsCaptor.getValue();
        assertEquals(List.of("p-1", "p-2"), links.stream().map(ProblemParagraphEntity::getParagraphId).toList());
        assertEquals(1, links.stream().map(ProblemParagraphEntity::getProblemId).distinct().count());

        // 索引事件：携带知识库 ID、文档 ID 列表与初始状态
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(KNOWLEDGE_ID, eventCaptor.getValue().getKnowledgeId());
        assertEquals(List.of(savedDoc.getId()), eventCaptor.getValue().getDocIds());
        assertEquals(List.of("0"), eventCaptor.getValue().getStateList());
    }

    @Test
    void batchCreateDocs_existingKnowledgeProblem_reusesItsId() {
        ProblemEntity existing = new ProblemEntity();
        existing.setId("problem-exists");
        existing.setContent("已存在问题");
        stubExistingProblems(List.of(existing));
        stubProblemLookup();
        stubParagraphCreation();
        doReturn(true).when(documentWriteService).saveBatch(anyList());

        ParagraphSimple paragraph = paragraph("标题", "内容", List.of("已存在问题"));
        DocumentSimple doc = new DocumentSimple("文档", null);
        doc.setParagraphs(List.of(paragraph));

        documentWriteService.batchCreateDocs(KNOWLEDGE_ID, KnowledgeType.BASE, List.of(doc));

        // 命中知识库既有问题：不再新建，只建立问题-段落关联
        verify(problemService, never()).saveBatch(anyList());
        verify(problemParagraphService).saveBatch(problemParagraphsCaptor.capture());
        assertEquals("problem-exists", problemParagraphsCaptor.getValue().get(0).getProblemId());
        verify(eventPublisher).publishEvent(any(DocumentIndexEvent.class));
    }

    private void stubExistingProblems(List<ProblemEntity> existing) {
        // LambdaQueryChainWrapper 的泛型链式方法（eq）无法被 RETURNS_DEEP_STUBS 解析，显式打桩
        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ProblemEntity> chain = mock(LambdaQueryChainWrapper.class);
        when(problemService.lambdaQuery()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.list()).thenReturn(new ArrayList<>(existing));
    }

    /**
     * 复现 {@code ProblemServiceImpl#findProblem} 的语义：在知识库已知问题中按内容匹配，未命中返回 null。
     */
    private void stubProblemLookup() {
        when(problemService.findProblem(anyString(), anyList())).thenAnswer(invocation -> {
            String content = invocation.getArgument(0);
            List<ProblemEntity> known = invocation.getArgument(1);
            return known.stream()
                    .filter(p -> content.equals(p.getContent()))
                    .findFirst()
                    .orElse(null);
        });
    }

    /**
     * 段落创建委托打桩：为每个段落生成独立 ID，供问题-段落关联使用。
     */
    private void stubParagraphCreation() {
        AtomicInteger paragraphSeq = new AtomicInteger();
        when(paragraphService.createParagraph(eq(KNOWLEDGE_ID), anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    ParagraphEntity paragraph = new ParagraphEntity();
                    paragraph.setId("p-" + paragraphSeq.incrementAndGet());
                    return paragraph;
                });
    }

    private static ParagraphSimple paragraph(String title, String content, List<String> problems) {
        return ParagraphSimple.builder()
                .title(title)
                .content(content)
                .problemList(problems)
                .build();
    }
}
