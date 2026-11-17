package com.maxkb4j.application.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationChatRecordMapper;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;
import com.maxkb4j.knowledge.service.IParagraphService;
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

    private static final String CHAT_ID = "chat-owned";
    private static final String RECORD_ID = "record-1";

    @Mock
    private IParagraphService paragraphService;
    @Mock
    private ApplicationChatMapper chatMapper;
    @Mock
    private ApplicationChatRecordMapper recordMapper;

    private ApplicationChatRecordServiceImpl recordService;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境下 MyBatis-Plus 的 LambdaWrapper 解析列名依赖 TableInfo 缓存，需手动初始化
        if (TableInfoHelper.getTableInfo(ApplicationChatRecordEntity.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            TableInfoHelper.initTableInfo(assistant, ApplicationChatRecordEntity.class);
        }
    }

    @BeforeEach
    void setUp() {
        recordService = new ApplicationChatRecordServiceImpl(paragraphService, chatMapper);
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

    /**
     * wrapper 的条件值在 SQL 片段渲染时才写入 paramNameValuePairs，mock 环境下需手动触发一次渲染
     */
    private static void renderSqlSegment(LambdaQueryWrapper<ApplicationChatRecordEntity> wrapper) {
        wrapper.getSqlSegment();
    }
}
