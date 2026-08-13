package com.maxkb4j.common.cache;

import com.maxkb4j.common.domain.dto.ChatInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatCache 静态门面默认行为测试（第 3 期组件化）。
 */
class ChatCacheTest {

    @AfterEach
    void tearDown() {
        ChatCache.reset();
    }

    @Test
    void defaultStore_putGetRemove() {
        ChatInfo chatInfo = new ChatInfo("chat-1", "app-1");
        ChatCache.put("chat-1", chatInfo);

        assertThat(ChatCache.get("chat-1")).isSameAs(chatInfo);

        ChatCache.remove("chat-1");
        assertThat(ChatCache.get("chat-1")).isNull();
    }

    @Test
    void init_overridesStore() {
        ChatInfoStore fake = new ChatInfoStore() {
            private ChatInfo stored;

            @Override
            public void put(String chatId, ChatInfo chatInfo) {
                stored = chatInfo;
            }

            @Override
            public ChatInfo get(String chatId) {
                return stored;
            }

            @Override
            public void remove(String chatId) {
                stored = null;
            }
        };
        ChatCache.init(fake);
        ChatInfo chatInfo = new ChatInfo("chat-9", "app-9");

        ChatCache.put("chat-9", chatInfo);

        assertThat(ChatCache.get("chat-9")).isSameAs(chatInfo);
    }
}