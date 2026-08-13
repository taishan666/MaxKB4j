package com.maxkb4j.application.cache;

import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 聊天会话缓存 Bean + 静态门面行为测试（第 3 期组件化）。
 */
class ChatInfoCacheServiceTest {

    @AfterEach
    void tearDown() {
        ChatCache.reset();
    }

    @Test
    void putGetRemove_roundTrip() {
        ChatInfoCacheService service = new ChatInfoCacheService();
        ChatInfo chatInfo = new ChatInfo("chat-1", "app-1");

        service.put("chat-1", chatInfo);

        assertThat(service.get("chat-1")).isSameAs(chatInfo);
        service.remove("chat-1");
        assertThat(service.get("chat-1")).isNull();
    }

    @Test
    void construct_registersBeanIntoFacade() {
        new ChatInfoCacheService();
        ChatInfo chatInfo = new ChatInfo("chat-2", "app-1");

        ChatCache.put("chat-2", chatInfo);

        assertThat(ChatCache.get("chat-2")).isSameAs(chatInfo);
        ChatCache.remove("chat-2");
        assertThat(ChatCache.get("chat-2")).isNull();
    }
}