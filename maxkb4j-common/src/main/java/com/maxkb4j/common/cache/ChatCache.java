package com.maxkb4j.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.domain.dto.ChatInfo;

import java.util.concurrent.TimeUnit;

/**
 * 聊天会话缓存静态门面（统一实现）。
 *
 * <p>默认内置 Caffeine 本地缓存，chat / workflow / application 各模块通过静态 API
 * 读写会话信息；如需替换存储实现，可通过 {@link #init(ChatInfoStore)} 注册。</p>
 */
public final class ChatCache {

    private static volatile ChatInfoStore store = new CaffeineChatInfoStore();

    private ChatCache() {
    }

    /** 注册存储实现（后注册者生效）。 */
    public static void init(ChatInfoStore newStore) {
        store = newStore;
    }

    /** 恢复默认 Caffeine 实现，供测试隔离使用。 */
    public static void reset() {
        store = new CaffeineChatInfoStore();
    }

    public static void put(String chatId, ChatInfo chatInfo) {
        store.put(chatId, chatInfo);
    }

    public static ChatInfo get(String chatId) {
        return store.get(chatId);
    }

    public static void remove(String chatId) {
        store.remove(chatId);
    }

    /** 默认实现：Caffeine 本地缓存。 */
    private static final class CaffeineChatInfoStore implements ChatInfoStore {

        /** 缓存最大容量（会话数），超出后由 Caffeine 按策略淘汰。 */
        private static final int MAXIMUM_SIZE = 9999;

        /** 会话过期时间（分钟），写入后计时。 */
        private static final int EXPIRE_AFTER_WRITE_MINUTES = 1440;

        private final Cache<String, ChatInfo> cache = Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
                .build();

        @Override
        public void put(String chatId, ChatInfo chatInfo) {
            cache.put(chatId, chatInfo);
        }

        @Override
        public ChatInfo get(String chatId) {
            return cache.getIfPresent(chatId);
        }

        @Override
        public void remove(String chatId) {
            cache.invalidate(chatId);
        }
    }
}