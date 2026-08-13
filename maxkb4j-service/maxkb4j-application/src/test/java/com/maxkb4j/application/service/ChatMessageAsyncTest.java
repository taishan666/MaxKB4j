package com.maxkb4j.application.service;

import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;
import com.maxkb4j.application.handler.PostResponseHandler;
import com.maxkb4j.application.mapper.ApplicationChatShareLinkMapper;
import com.maxkb4j.application.service.impl.ApplicationChatServiceImpl;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * chatMessageAsync 错误传播与 SSE 收尾测试。
 * <p>
 * 使用同步执行器（Runnable::run），使异步路径在测试线程内确定性完成：
 * <ul>
 *   <li>业务抛异常 → sink 收到唯一一次 error 终止信号；</li>
 *   <li>业务自行 emit 错误后正常返回 → 终止信号保持为业务的错误，不被二次覆盖。</li>
 * </ul>
 * </p>
 */
class ChatMessageAsyncTest {

    private static final String CHAT_ID = "chat-async-test";

    @AfterEach
    void tearDown() {
        ChatCache.remove(CHAT_ID);
    }

    private ApplicationChatServiceImpl newChatService(ApplicationChatUserStatsService statsServiceMock,
                                                      IApplicationService applicationService) {
        return new ApplicationChatServiceImpl(
                mock(IApplicationChatRecordInternalService.class),
                applicationService,
                statsServiceMock,
                mock(IApplicationAccessTokenInternalService.class),
                mock(ApplicationVersionService.class),
                mock(PostResponseHandler.class),
                Runnable::run,
                mock(ApplicationChatShareLinkMapper.class));
    }

    private static ChatParams chatParams() {
        return ChatParams.builder().chatId(CHAT_ID).message("hello").reChat(false).stream(true).build();
    }

    private static ChatState chatState() {
        return ChatState.builder()
                .appId("app-1")
                .chatUserId("user-1")
                .debug(false)
                .build();
    }

    private static Throwable awaitTermination(Sinks.Many<ChatMessageVO> sink) throws InterruptedException {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        sink.asFlux().subscribe(
                message -> { },
                error -> {
                    errorRef.set(error);
                    latch.countDown();
                },
                latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "SSE 流应在超时前终止");
        return errorRef.get();
    }

    @Test
    void chatMessageAsync_businessException_terminatesSinkWithError() throws Exception {
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        when(statsServiceMock.ensureStatsExists(anyString(), any(), anyString()))
                .thenThrow(new IllegalStateException("stats backend unavailable"));
        ApplicationChatServiceImpl chatService = newChatService(statsServiceMock, mock(IApplicationService.class));

        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        chatService.chatMessageAsync(chatParams(), chatState(), sink);

        Throwable error = awaitTermination(sink);
        assertNotNull(error, "业务异常应转换为 sink 的 error 终止信号");
        Throwable rootCause = error.getCause() != null ? error.getCause() : error;
        assertInstanceOf(IllegalStateException.class, rootCause);
    }

    @Test
    void chatMessageAsync_businessEmittedError_isNotOverridden() throws Exception {
        // visitCountOver 通过（首次访问），但应用不存在 → chatMessage 自行 emit ApiException 后正常返回
        ApplicationChatUserStatsService statsServiceMock = mock(ApplicationChatUserStatsService.class);
        when(statsServiceMock.ensureStatsExists(anyString(), any(), anyString())).thenReturn(true);
        IApplicationService applicationService = mock(IApplicationService.class);
        when(applicationService.getAppDetail(anyString(), anyBoolean())).thenReturn(null);
        ApplicationChatServiceImpl chatService = newChatService(statsServiceMock, applicationService);
        ChatCache.put(CHAT_ID, new ChatInfo(CHAT_ID, "app-1"));

        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        chatService.chatMessageAsync(chatParams(), chatState(), sink);

        Throwable error = awaitTermination(sink);
        assertInstanceOf(ApiException.class, error, "业务侧 emit 的错误应保持为唯一终止信号");
    }
}