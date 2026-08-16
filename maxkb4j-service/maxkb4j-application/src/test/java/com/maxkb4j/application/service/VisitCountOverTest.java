package com.maxkb4j.application.service;

import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;
import com.maxkb4j.application.entity.ApplicationChatUserStatsEntity;
import com.maxkb4j.application.handler.PostResponseHandler;
import com.maxkb4j.application.mapper.ApplicationChatShareLinkMapper;
import com.maxkb4j.application.mapper.ApplicationChatUserStatsMapper;
import com.maxkb4j.application.service.impl.ApplicationChatServiceImpl;
import com.maxkb4j.common.cache.ChatInfoCacheService;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.enums.ChatUserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * visitCountOver 限流与统计行 upsert 测试。
 * <p>
 * 数据库层保证来自 V8__chat_user_stats_unique.sql 的唯一索引 (chat_user_id, application_id)，
 * 本测试以 fake mapper 模拟该唯一索引语义，验证并发下服务层只产生一行统计、限流判断正确。
 * </p>
 */
class VisitCountOverTest {

    private ApplicationChatUserStatsMapper statsMapper;
    private ApplicationChatUserStatsService statsService;

    @BeforeEach
    void setUpStatsService() {
        statsMapper = mock(ApplicationChatUserStatsMapper.class);
        statsService = new ApplicationChatUserStatsService();
        ReflectionTestUtils.setField(statsService, "baseMapper", statsMapper);
    }

    private ApplicationChatServiceImpl newChatService(ApplicationChatUserStatsService statsServiceMock,
                                                      IApplicationAccessTokenInternalService accessTokenService) {
        return new ApplicationChatServiceImpl(
                mock(IApplicationChatRecordInternalService.class),
                mock(IApplicationService.class),
                statsServiceMock,
                accessTokenService,
                mock(ApplicationVersionService.class),
                mock(PostResponseHandler.class),
                mock(TaskExecutor.class),
                mock(ApplicationChatShareLinkMapper.class),
                new ChatInfoCacheService());
    }

    private static ChatState.ChatStateBuilder chatState() {
        return ChatState.builder()
                .appId("app-1")
                .chatUserId("user-1")
                .chatUserType(ChatUserType.ANONYMOUS_USER)
                .debug(false);
    }

    private static ApplicationChatUserStatsEntity statsWithIntraDay(int intraDayAccessNum) {
        ApplicationChatUserStatsEntity entity = new ApplicationChatUserStatsEntity();
        entity.setChatUserId("user-1");
        entity.setApplicationId("app-1");
        entity.setAccessNum(intraDayAccessNum);
        entity.setIntraDayAccessNum(intraDayAccessNum);
        return entity;
    }

    private static ApplicationAccessTokenEntity tokenWithLimit(Integer accessNum) {
        ApplicationAccessTokenEntity token = new ApplicationAccessTokenEntity();
        token.setApplicationId("app-1");
        token.setAccessNum(accessNum);
        return token;
    }

    // ==================== ensureStatsExists：并发下只建一行 ====================

    @Test
    void ensureStatsExists_concurrent_createsExactlyOneRow() throws Exception {
        // 模拟 DB 唯一索引 (chat_user_id, application_id) 语义：仅首次插入返回 1
        Set<String> tableRows = ConcurrentHashMap.newKeySet();
        when(statsMapper.insertIfAbsent(any(ApplicationChatUserStatsEntity.class))).thenAnswer(invocation -> {
            ApplicationChatUserStatsEntity entity = invocation.getArgument(0);
            boolean inserted = tableRows.add(entity.getChatUserId() + "|" + entity.getApplicationId());
            return inserted ? 1 : 0;
        });

        int threadCount = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(pool.submit(() ->
                        statsService.ensureStatsExists("user-1", ChatUserType.ANONYMOUS_USER, "app-1")));
            }
            int createdCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) {
                    createdCount++;
                }
            }
            assertEquals(1, createdCount, "只有一个线程应判定为新建统计行");
            assertEquals(1, tableRows.size(), "同一用户+应用只能存在一行统计");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void ensureStatsExists_returnsFalseWhenRowAlreadyExists() {
        when(statsMapper.insertIfAbsent(any(ApplicationChatUserStatsEntity.class))).thenReturn(0);
        assertFalse(statsService.ensureStatsExists("user-1", ChatUserType.ANONYMOUS_USER, "app-1"));
    }

    @Test
    void incrementAccessNum_delegatesToAtomicUpdate() {
        when(statsMapper.incrementAccessNum("user-1", "app-1")).thenReturn(1);
        assertEquals(1, statsService.incrementAccessNum("user-1", "app-1"));
    }

    // ==================== visitCountOver：限流语义 ====================

    @Test
    void visitCountOver_debugMode_skipsStats() {
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        ApplicationChatServiceImpl chatService =
                newChatService(statsServiceMock, mock(IApplicationAccessTokenInternalService.class));
        boolean over = chatService.visitCountOver(chatState().debug(true).build());
        assertFalse(over);
        verifyNoInteractions(statsServiceMock);
    }

    @Test
    void visitCountOver_missingAppId_skipsStats() {
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        ApplicationChatServiceImpl chatService =
                newChatService(statsServiceMock, mock(IApplicationAccessTokenInternalService.class));
        boolean over = chatService.visitCountOver(chatState().appId(null).build());
        assertFalse(over);
        verifyNoInteractions(statsServiceMock);
    }

    @Test
    void visitCountOver_firstVisit_allowedWithoutLimitCheck() {
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        IApplicationAccessTokenInternalService accessTokenService = mock(IApplicationAccessTokenInternalService.class);
        when(statsServiceMock.ensureStatsExists(anyString(), any(), anyString())).thenReturn(true);
        ApplicationChatServiceImpl chatService = newChatService(statsServiceMock, accessTokenService);

        assertFalse(chatService.visitCountOver(chatState().build()));
        verifyNoInteractions(accessTokenService);
    }

    @Test
    void visitCountOver_noAccessToken_allowed() {
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        IApplicationAccessTokenInternalService accessTokenService = mock(IApplicationAccessTokenInternalService.class);
        when(statsServiceMock.ensureStatsExists(anyString(), any(), anyString())).thenReturn(false);
        when(accessTokenService.accessToken("app-1")).thenReturn(null);
        ApplicationChatServiceImpl chatService = newChatService(statsServiceMock, accessTokenService);

        assertFalse(chatService.visitCountOver(chatState().build()));
    }

    @Test
    void visitCountOver_underLimit_allowed() {
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        IApplicationAccessTokenInternalService accessTokenService = mock(IApplicationAccessTokenInternalService.class);
        when(statsServiceMock.ensureStatsExists(anyString(), any(), anyString())).thenReturn(false);
        when(accessTokenService.accessToken("app-1")).thenReturn(tokenWithLimit(5));
        when(statsServiceMock.getByUserIdAndAppId("user-1", "app-1")).thenReturn(statsWithIntraDay(4));
        ApplicationChatServiceImpl chatService = newChatService(statsServiceMock, accessTokenService);

        assertFalse(chatService.visitCountOver(chatState().build()));
    }

    @Test
    void visitCountOver_reachesLimit_blocked() {
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        IApplicationAccessTokenInternalService accessTokenService = mock(IApplicationAccessTokenInternalService.class);
        when(statsServiceMock.ensureStatsExists(anyString(), any(), anyString())).thenReturn(false);
        when(accessTokenService.accessToken("app-1")).thenReturn(tokenWithLimit(5));
        when(statsServiceMock.getByUserIdAndAppId("user-1", "app-1")).thenReturn(statsWithIntraDay(5));
        ApplicationChatServiceImpl chatService = newChatService(statsServiceMock, accessTokenService);

        assertTrue(chatService.visitCountOver(chatState().build()));
    }

    @Test
    void visitCountOver_nullLimitValue_allowed() {
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        IApplicationAccessTokenInternalService accessTokenService = mock(IApplicationAccessTokenInternalService.class);
        when(statsServiceMock.ensureStatsExists(anyString(), any(), anyString())).thenReturn(false);
        when(accessTokenService.accessToken("app-1")).thenReturn(tokenWithLimit(null));
        ApplicationChatServiceImpl chatService = newChatService(statsServiceMock, accessTokenService);

        assertFalse(chatService.visitCountOver(chatState().build()));
    }
}