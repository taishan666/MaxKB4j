package com.maxkb4j.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.maxkb4j.application.dto.ShareChatDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationChatRecordMapper;
import com.maxkb4j.application.mapper.ApplicationChatShareLinkMapper;
import com.maxkb4j.application.service.*;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.application.handler.PostResponseHandler;
import org.junit.jupiter.api.AfterEach;
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
}
