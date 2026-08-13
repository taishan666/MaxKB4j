package com.maxkb4j.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.domain.dto.ChatInfo;

import java.util.concurrent.TimeUnit;

public class ChatCache {

    /** 缓存最大容量（会话数），超出后由 Caffeine 按策略淘汰。 */
    private static final int MAXIMUM_SIZE = 9999;

    /** 会话过期时间（分钟），写入后计时。 */
    private static final int EXPIRE_AFTER_WRITE_MINUTES = 30;

    // 创建缓存实例，设置最大容量与过期时间
    private static final Cache<String, ChatInfo> CHAT_CACHE = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_SIZE)
            .expireAfterWrite(EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
            .build();

    public static void put(String chatId, ChatInfo chatInfo) {
        CHAT_CACHE.put(chatId, chatInfo);
    }

    public static ChatInfo get(String chatId) {
        return CHAT_CACHE.getIfPresent(chatId);
    }
    public static void remove(String chatId) {
        CHAT_CACHE.invalidate(chatId);
    }
}
