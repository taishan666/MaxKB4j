package com.maxkb4j.application.service;

import com.maxkb4j.application.handler.PostResponseHandler;
import com.maxkb4j.application.mapper.ApplicationChatShareLinkMapper;
import com.maxkb4j.application.service.impl.ApplicationChatServiceImpl;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import org.junit.jupiter.api.AfterEach;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
                message -> {
                },
                error -> {
                    errorRef.set(error);
                    latch.countDown();
                },
                latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "SSE 流应在超时前终止");
        return errorRef.get();
    }

    @AfterEach
    void tearDown() {
        ChatCache.remove(CHAT_ID);
    }

    private ApplicationChatServiceImpl newChatService(ApplicationChatUserStatsService statsServiceMock,
                                                      IApplicationInternalService applicationService) {
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



}