package com.maxkb4j.application.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.service.ApplicationChatUserStatsService;
import com.maxkb4j.application.service.IApplicationChatRecordInternalService;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.domain.dto.ChatState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * ChatPostHandler 事务与缓存一致性测试。
 * <p>
 * 数据库层保证来自 application_chat 主键唯一约束 + INSERT ... ON CONFLICT 原子 upsert，
 * 本测试验证：缓存写入推迟到事务提交后（回滚不留"幽灵回答"），会话保存走单条原子 upsert
 * （不再"先查后插、冲突回退 UPDATE"——PostgreSQL 中语句失败会中止整个事务）。
 * </p>
 */
class ChatPostHandlerTest {

    private static final String CHAT_ID = "chat-post-test";
    private static final String CHAT_RECORD_ID = "record-1";

    private ApplicationChatMapper chatMapper;
    private ApplicationChatUserStatsService statsService;
    private IApplicationChatRecordInternalService chatRecordService;
    private ChatPostHandler handler;

    @BeforeEach
    void setUp() {
        chatMapper = mock(ApplicationChatMapper.class);
        statsService = mock(ApplicationChatUserStatsService.class);
        chatRecordService = mock(IApplicationChatRecordInternalService.class);
        handler = new ChatPostHandler(statsService, chatMapper, chatRecordService);
    }

    @AfterEach
    void tearDown() {
        ChatCache.remove(CHAT_ID);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static ChatParams chatParams() {
        return ChatParams.builder()
                .chatId(CHAT_ID)
                .chatRecordId(CHAT_RECORD_ID)
                .message("hello")
                .build();
    }

    private static ChatState.ChatStateBuilder chatState() {
        return ChatState.builder()
                .appId("app-1")
                .chatUserId("user-1")
                .debug(false);
    }

    private static ChatResponse chatResponse() {
        // runDetails 为空 JSONObject：sumTokenField 直接遍历其 values，传 null 会 NPE
        return new ChatResponse(List.of(), new JSONObject());
    }

    // ==================== 缓存与事务一致性 ====================

    @Test
    void handler_persistFails_cacheKeepsNoGhostRecord() {
        // 模拟事务上下文；持久化失败 → 事务回滚，缓存不应出现未落库的"幽灵回答"
        TransactionSynchronizationManager.initSynchronization();
        doThrow(new RuntimeException("db down")).when(chatRecordService).saveOrUpdate(any(ApplicationChatRecordEntity.class));

        assertThrows(RuntimeException.class,
                () -> handler.handler(chatParams(), chatState().build(), chatResponse(), 0L));

        ChatInfo chatInfo = ChatCache.get(CHAT_ID);
        assertTrue(chatInfo == null || chatInfo.getChatRecordList().isEmpty(), "回滚后缓存不应留下未持久化的对话记录");
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty(), "持久化失败时不应注册提交后回调");
    }

    @Test
    void handler_cacheUpdatedOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        handler.handler(chatParams(), chatState().build(), chatResponse(), 0L);

        ChatInfo chatInfo = ChatCache.get(CHAT_ID);
        assertTrue(chatInfo != null && chatInfo.getChatRecordList().isEmpty(), "提交前缓存不应包含对话记录");
        // 模拟事务提交，触发 afterCommit 回调
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        List<ChatRecordDTO> records = ChatCache.get(CHAT_ID).getChatRecordList();
        assertEquals(1, records.size(), "提交后缓存应包含本轮对话记录");
        assertEquals(CHAT_RECORD_ID, records.get(0).getId());
    }

    @Test
    void handler_noTransactionContext_updatesCacheImmediately() {
        // 直调（无事务代理）场景：立即写缓存，等价于提交后的最终状态
        handler.handler(chatParams(), chatState().build(), chatResponse(), 0L);

        List<ChatRecordDTO> records = ChatCache.get(CHAT_ID).getChatRecordList();
        assertEquals(1, records.size(), "无事务上下文时应立即写入缓存");
        assertEquals(CHAT_RECORD_ID, records.get(0).getId());
    }

    // ==================== 会话保存：单条原子 upsert ====================

    @Test
    void handler_newRecord_upsertsChatAtomically() {
        TransactionSynchronizationManager.initSynchronization();

        handler.handler(chatParams(), chatState().build(), chatResponse(), 0L);

        verify(statsService).incrementAccessNum("user-1", "app-1");
        verify(chatMapper).insertOrIncrementRecordCount(any(ApplicationChatEntity.class));
        // 不再走"先查后插、冲突回退 UPDATE"：对 chatMapper 只有这一条语句
        verifyNoMoreInteractions(chatMapper);
    }

    @Test
    void handler_concurrentCreate_conflictResolvedBySingleUpsert() {
        // 会话行已被并发请求创建时（DB 侧 ON CONFLICT 原子 +1），应用层仍是同一条 upsert 语句
        TransactionSynchronizationManager.initSynchronization();

        handler.handler(chatParams(), chatState().build(), chatResponse(), 0L);
        handler.handler(ChatParams.builder()
                        .chatId(CHAT_ID).chatRecordId("record-2").message("again").build(),
                chatState().build(), chatResponse(), 0L);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(chatMapper, times(2)).insertOrIncrementRecordCount(any(ApplicationChatEntity.class));
        verifyNoMoreInteractions(chatMapper);
        assertEquals(2, ChatCache.get(CHAT_ID).getChatRecordList().size(), "同会话两轮对话均应在提交后进入缓存");
    }

    // ==================== 补充/覆盖已有记录与调试模式 ====================

    @Test
    void handler_reChat_skipsChatUpsertAndStats() {
        ChatRecordDTO existing = new ChatRecordDTO();
        existing.setId(CHAT_RECORD_ID);
        existing.setIndex(1);
        existing.setRunTime(1.0F);
        existing.setVoteStatus("0");

        handler.handler(chatParams(), chatState().chatRecord(existing).build(), chatResponse(), 0L);

        // 覆盖已有记录：会话必然已创建，不重复累加 chat_record_count 与访问计数
        verifyNoInteractions(chatMapper, statsService);
        verify(chatRecordService).saveOrUpdate(any(ApplicationChatRecordEntity.class));
    }

    @Test
    void handler_debugMode_skipsPersistenceButCachesRecord() {
        handler.handler(chatParams(), chatState().debug(true).build(), chatResponse(), 0L);

        verifyNoInteractions(chatMapper, chatRecordService, statsService);
        List<ChatRecordDTO> records = ChatCache.get(CHAT_ID).getChatRecordList();
        assertEquals(1, records.size(), "调试模式下记录仅进缓存不落库");
        assertNotNull(records.get(0).getId(), "缓存中的记录 id 不应为 null");
    }
}
