package com.maxkb4j.common.cache;

import com.maxkb4j.common.domain.dto.ChatInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 聊天会话缓存 Bean 行为测试（第 3 期组件化）。
 */
class ChatInfoCacheServiceTest {

    @Test
    void putGetRemove_roundTrip() {
        ChatInfoCacheService service = new ChatInfoCacheService();
        ChatInfo chatInfo = new ChatInfo("chat-1", "app-1");

        service.put("chat-1", chatInfo);

        assertThat(service.get("chat-1")).isSameAs(chatInfo);
        service.remove("chat-1");
        assertThat(service.get("chat-1")).isNull();
    }
}