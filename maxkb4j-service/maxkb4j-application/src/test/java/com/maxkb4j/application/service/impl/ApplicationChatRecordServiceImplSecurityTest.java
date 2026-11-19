package com.maxkb4j.application.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.maxkb4j.application.dto.AddChatImproveDTO;
import com.maxkb4j.application.dto.ChatImproveDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationChatRecordMapper;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;
import com.maxkb4j.knowledge.dto.ParagraphDTO;
import com.maxkb4j.knowledge.service.IDocumentService;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.system.service.IResourceMappingService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationChatRecordServiceImplSecurityTest {

    private static final String APP_ID = "app-owned";
    private static final String CHAT_ID = "chat-owned";
    private static final String RECORD_ID = "record-1";
    private static final String KNOWLEDGE_ID = "knowledge-1";
    private static final String DOC_ID = "doc-1";
    private static final String PARAGRAPH_ID = "paragraph-1";

    @Mock
    private IParagraphService paragraphService;
    @Mock
    private ApplicationChatMapper chatMapper;
    @Mock
    private ApplicationChatRecordMapper recordMapper;
    @Mock
    private IDocumentService documentService;
    @Mock
    private IResourceMappingService resourceMappingService;

    private ApplicationChatRecordServiceImpl recordService;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境下 MyBatis-Plus 的 LambdaWrapper 解析列名依赖 TableInfo 缓存，需手动初始化
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        if (TableInfoHelper.getTableInfo(ApplicationChatRecordEntity.class) == null) {
            TableInfoHelper.initTableInfo(assistant, ApplicationChatRecordEntity.class);
        }
        if (TableInfoHelper.getTableInfo(ApplicationChatEntity.class) == null) {
            TableInfoHelper.initTableInfo(assistant, ApplicationChatEntity.class);
        }
    }

    @BeforeEach
    void setUp() {
        recordService = new ApplicationChatRecordServiceImpl(
                paragraphService, chatMapper, documentService, resourceMappingService);
        ReflectionTestUtils.setField(recordService, "baseMapper", recordMapper);
    }

    // ============ getChatRecordInfo DB fallback 包含 chatId ============

    @Test
    void getChatRecordInfo_dbFallback_includesChatId() {
        when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertNull(recordService.getChatRecordInfo(CHAT_ID, RECORD_ID));

        // selectOne 参数中应包含 chat_id 和 id 条件
        ArgumentCaptor<LambdaQueryWrapper<ApplicationChatRecordEntity>> captor = ArgumentCaptor.captor();
        verify(recordMapper).selectOne(captor.capture());
        LambdaQueryWrapper<ApplicationChatRecordEntity> wrapper = captor.getValue();
        renderSqlSegment(wrapper);
        assertTrue(wrapper.getParamNameValuePairs().containsValue(CHAT_ID));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(RECORD_ID));
    }

    // ============ listVOByIds 包含 chatId ============

    @Test
    void listVOByIds_includesChatId() {
        when(recordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<ApplicationChatRecordVO> result = recordService.listVOByIds(CHAT_ID, List.of(RECORD_ID));

        assertTrue(result.isEmpty());
        ArgumentCaptor<LambdaQueryWrapper<ApplicationChatRecordEntity>> captor = ArgumentCaptor.captor();
        verify(recordMapper).selectList(captor.capture());
        LambdaQueryWrapper<ApplicationChatRecordEntity> wrapper = captor.getValue();
        renderSqlSegment(wrapper);
        assertTrue(wrapper.getParamNameValuePairs().containsValue(CHAT_ID));
    }

    @Test
    void chatRecordPage_foreignChat_returnsEmptyWithoutQueryingRecords() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        IPage<ApplicationChatRecordVO> result = recordService.chatRecordPage(APP_ID, CHAT_ID, 1, 10);

        assertTrue(result.getRecords().isEmpty());
        verify(recordMapper, never()).selectPage(any(), any());
    }

    @Test
    void addChatLogs_mixedChatOwnership_rejectsEntireBatch() {
        when(resourceMappingService.existsMapping(any(), any(), any(), any())).thenReturn(true);
        when(documentService.existsByKnowledgeIdAndId(KNOWLEDGE_ID, DOC_ID)).thenReturn(true);
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        AddChatImproveDTO dto = new AddChatImproveDTO();
        dto.setChatIds(List.of(CHAT_ID, "chat-foreign"));
        dto.setKnowledgeId(KNOWLEDGE_ID);
        dto.setDocumentId(DOC_ID);

        assertFalse(recordService.addChatLogs(APP_ID, dto));

        verify(recordMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(paragraphService, never()).saveDtoBatch(anyList());
    }

    @Test
    void improveChatLog_foreignKnowledge_doesNotWriteParagraphOrRecord() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        ApplicationChatRecordEntity ownedRecord = new ApplicationChatRecordEntity();
        ownedRecord.setId(RECORD_ID);
        ownedRecord.setChatId(CHAT_ID);
        when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownedRecord);
        when(resourceMappingService.existsMapping(any(), any(), any(), any())).thenReturn(false);
        ChatImproveDTO dto = new ChatImproveDTO();
        dto.setTitle("title");
        dto.setContent("content");
        dto.setProblemText("problem");

        assertNull(recordService.improveChatLog(APP_ID, CHAT_ID, RECORD_ID, KNOWLEDGE_ID, DOC_ID, dto));

        verify(paragraphService, never()).saveParagraphAndProblem(any(), anyList());
        verify(recordMapper, never()).update(any(), any());
    }

    @Test
    void removeImproveChatLog_paragraphOutsideRecord_doesNotDelete() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        ApplicationChatRecordEntity ownedRecord = new ApplicationChatRecordEntity();
        ownedRecord.setImproveParagraphIdList(List.of("paragraph-other"));
        when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownedRecord);
        when(resourceMappingService.existsMapping(any(), any(), any(), any())).thenReturn(true);
        when(documentService.existsByKnowledgeIdAndId(KNOWLEDGE_ID, DOC_ID)).thenReturn(true);

        assertFalse(recordService.removeImproveChatLog(
                APP_ID, CHAT_ID, RECORD_ID, KNOWLEDGE_ID, DOC_ID, PARAGRAPH_ID));

        verify(paragraphService, never()).deleteById(any(), any());
    }

    @Test
    void removeImproveChatLog_paragraphOutsideDocument_doesNotDelete() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        ApplicationChatRecordEntity ownedRecord = new ApplicationChatRecordEntity();
        ownedRecord.setImproveParagraphIdList(List.of(PARAGRAPH_ID));
        when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ownedRecord);
        when(resourceMappingService.existsMapping(any(), any(), any(), any())).thenReturn(true);
        when(documentService.existsByKnowledgeIdAndId(KNOWLEDGE_ID, DOC_ID)).thenReturn(true);
        ParagraphDTO foreignParagraph = new ParagraphDTO(
                PARAGRAPH_ID, KNOWLEDGE_ID, "doc-foreign", "title", "content", null);
        when(paragraphService.listDtoByIds(List.of(PARAGRAPH_ID))).thenReturn(List.of(foreignParagraph));

        assertFalse(recordService.removeImproveChatLog(
                APP_ID, CHAT_ID, RECORD_ID, KNOWLEDGE_ID, DOC_ID, PARAGRAPH_ID));

        verify(paragraphService, never()).deleteById(any(), any());
    }

    @Test
    void improveChatLog_foreignRecord_returnsEmptyWithoutQueryingParagraphs() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(recordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        List<ParagraphDTO> result = recordService.improveChatLog(APP_ID, CHAT_ID, RECORD_ID);

        assertTrue(result.isEmpty());
        verify(paragraphService, never()).listDtoByIds(anyList());
    }

    /**
     * wrapper 的条件值在 SQL 片段渲染时才写入 paramNameValuePairs，mock 环境下需手动触发一次渲染
     */
    private static void renderSqlSegment(LambdaQueryWrapper<ApplicationChatRecordEntity> wrapper) {
        wrapper.getSqlSegment();
    }
}
