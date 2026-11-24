package com.maxkb4j.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.domain.dto.ChatInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CaffeineChatInfoStore 默认行为测试。
 */
class CaffeineChatInfoStoreTest {

    @Test
    void putGetRemove_roundTrip() {
        ChatInfoStore store = new CaffeineChatInfoStore();
        ChatInfo chatInfo = new ChatInfo("chat-1", "app-1");
        store.put("chat-1", chatInfo);

        assertThat(store.get("chat-1")).isSameAs(chatInfo);

        store.remove("chat-1");
        assertThat(store.get("chat-1")).isNull();
    }

    @Test
    void injectedCache_isUsedForStorage() {
        Cache<String, ChatInfo> cache = Caffeine.newBuilder().maximumSize(10).build();
        ChatInfoStore store = new CaffeineChatInfoStore(cache);
        ChatInfo chatInfo = new ChatInfo("chat-9", "app-9");

        store.put("chat-9", chatInfo);

        assertThat(store.get("chat-9")).isSameAs(chatInfo);
        assertThat(cache.getIfPresent("chat-9")).isSameAs(chatInfo);
    }
}
