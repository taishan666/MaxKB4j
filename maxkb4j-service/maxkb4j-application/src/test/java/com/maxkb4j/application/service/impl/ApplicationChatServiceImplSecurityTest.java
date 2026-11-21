package com.maxkb4j.application.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.maxkb4j.application.dto.ShareChatDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.handler.PostResponseHandler;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationChatRecordMapper;
import com.maxkb4j.application.mapper.ApplicationChatShareLinkMapper;
import com.maxkb4j.application.service.*;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationChatServiceImplSecurityTest {

    private static final String CHAT_ID = "chat-001";
    private static final String APP_ID = "app-owned";
    private static final String USER_ID = "user-1";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IApplicationChatRecordInternalService chatRecordService;
    @Mock
    private IApplicationInternalService applicationService;
    @Mock
    private ApplicationChatUserStatsService statsService;
    @Mock
    private IApplicationAccessTokenInternalService accessTokenService;
    @Mock
    private ApplicationVersionService versionService;
    @Mock
    private ApplicationChatMapper chatMapper;
    @Mock
    private ApplicationChatRecordMapper chatRecordMapper;
    @Mock
    private ApplicationChatShareLinkMapper shareLinkMapper;

    private ApplicationChatServiceImpl chatService;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境下 MyBatis-Plus 的 LambdaWrapper 解析列名依赖 TableInfo 缓存，需手动初始化
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        if (TableInfoHelper.getTableInfo(ApplicationChatEntity.class) == null) {
            TableInfoHelper.initTableInfo(assistant, ApplicationChatEntity.class);
        }
    }

    @BeforeEach
    void setUp() {
        chatService = new ApplicationChatServiceImpl(
                chatRecordService,
                applicationService,
                statsService,
                accessTokenService,
                versionService,
                mock(PostResponseHandler.class),
                Runnable::run,
                shareLinkMapper);
        ReflectionTestUtils.setField(chatService, "baseMapper", chatMapper);
        // IRepository.lambdaQuery() 经 getEntityClass() 解析实体类型时依赖真实 MybatisMapperProxy，
        // Mockito mock mapper 走不通该反射路径，预置实体类型短路掉
        ReflectionTestUtils.setField(chatService, "entityClass", ApplicationChatEntity.class);
    }

    @AfterEach
    void tearDown() {
        ChatCache.remove(CHAT_ID);
    }

    // ============ shareChat 拒绝不属于当前应用的会话 ============

    @Test
    void shareChat_foreignApp_throws() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertThrows(ApiException.class,
                () -> chatService.shareChat(APP_ID, CHAT_ID, new ShareChatDTO()));
    }

    @Test
    void shareChat_foreignRecordIds_throws() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L); // belongsToApp → true
        // 深桩 mock 解析不了链式包装器的 Children 泛型返回值，改为在 mock mapper 上构造真实链
        when(chatRecordService.lambdaQuery()).thenReturn(new LambdaQueryChainWrapper<>(chatRecordMapper));
        ShareChatDTO dto = new ShareChatDTO();
        dto.setChatRecordIds(List.of("foreign-record"));
        assertThrows(ApiException.class,
                () -> chatService.shareChat(APP_ID, CHAT_ID, dto));
    }

    // ============ getOrCreateChatInfo（BUG-35：子应用派生会话的获取或初始化） ============

    @Test
    void getOrCreateChatInfo_cacheHit_returnsCachedWithoutDbQuery() {
        ChatInfo cached = new ChatInfo(CHAT_ID, APP_ID, USER_ID);
        ChatCache.put(CHAT_ID, cached);

        ChatInfo result = chatService.getOrCreateChatInfo(CHAT_ID, APP_ID, USER_ID);

        assertSame(cached, result);
        verify(chatMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void getOrCreateChatInfo_dbFallback_restoresHistoryFromDatabase() {
        ApplicationChatEntity entity = new ApplicationChatEntity();
        entity.setApplicationId(APP_ID);
        entity.setChatUserId(USER_ID);
        when(chatMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);
        when(chatRecordService.getChatRecords(CHAT_ID)).thenReturn(List.of());

        ChatInfo result = chatService.getOrCreateChatInfo(CHAT_ID, APP_ID, USER_ID);

        assertEquals(CHAT_ID, result.getChatId());
        assertEquals(APP_ID, result.getAppId());
        assertEquals(USER_ID, result.getChatUserId());
        // 回填缓存，后续请求直接走缓存
        assertSame(result, ChatCache.get(CHAT_ID));
    }

    @Test
    void getOrCreateChatInfo_notFound_createsNewSession() {
        when(chatMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ChatInfo result = chatService.getOrCreateChatInfo(CHAT_ID, APP_ID, USER_ID);

        assertEquals(CHAT_ID, result.getChatId());
        assertEquals(APP_ID, result.getAppId());
        assertEquals(USER_ID, result.getChatUserId());
        assertSame(result, ChatCache.get(CHAT_ID));
    }

    @Test
    void getOrCreateChatInfo_appMismatch_throws() {
        ChatCache.put(CHAT_ID, new ChatInfo(CHAT_ID, "other-app", USER_ID));

        assertThrows(ApiException.class,
                () -> chatService.getOrCreateChatInfo(CHAT_ID, APP_ID, USER_ID));
    }

    // ============ resolveChatRecord（BUG-38：最近 100 条之外的重聊记录） ============

    @Test
    void resolveChatRecord_cacheMiss_restoresRecordFromDatabase() {
        ChatInfo chatInfo = new ChatInfo(CHAT_ID, APP_ID, USER_ID);
        ChatRecordDTO stored = new ChatRecordDTO();
        stored.setId("old-record");
        when(chatRecordService.getChatRecord(CHAT_ID, "old-record")).thenReturn(stored);

        ChatRecordDTO result = chatService.resolveChatRecord(chatInfo, "old-record");

        assertSame(stored, result);
        verify(chatRecordService).getChatRecord(CHAT_ID, "old-record");
    }

    @Test
    void resolveChatRecord_notFound_rejectsInsteadOfTreatingAsNewRecord() {
        ChatInfo chatInfo = new ChatInfo(CHAT_ID, APP_ID, USER_ID);
        when(chatRecordService.getChatRecord(CHAT_ID, "missing-record")).thenReturn(null);

        assertThrows(ApiException.class,
                () -> chatService.resolveChatRecord(chatInfo, "missing-record"));
    }

    // ============ isOwnedBy ============

    @Test
    void isOwnedBy_matching_returnsTrue() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertTrue(chatService.isOwnedBy(CHAT_ID, APP_ID, USER_ID));
    }

    @Test
    void isOwnedBy_mismatch_returnsFalse() {
        when(chatMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertFalse(chatService.isOwnedBy(CHAT_ID, APP_ID, "other-user"));
    }

    @Test
    void chatExport_scopesChatIdsToApplication() throws Exception {
        List<String> chatIds = List.of(CHAT_ID, "chat-foreign");
        when(chatMapper.chatRecordDetail(APP_ID, chatIds)).thenReturn(List.of());
        HttpServletResponse response = mock(HttpServletResponse.class);
        ApplicationChatServiceImpl exportService = spy(chatService);
        doNothing().when(exportService).writeChatExport(anyList(), eq(response));

        exportService.chatExport(APP_ID, chatIds, response);

        verify(chatMapper).chatRecordDetail(APP_ID, chatIds);
        verify(exportService).writeChatExport(List.of(), response);
    }
}
