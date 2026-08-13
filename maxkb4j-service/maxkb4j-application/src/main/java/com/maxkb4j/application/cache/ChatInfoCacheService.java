package com.maxkb4j.application.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.cache.ChatInfoStore;
import com.maxkb4j.common.domain.dto.ChatInfo;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 聊天会话缓存 Bean 化实现（第 3 期）。
 * <p>构造器注入 Caffeine 实例，测试可替换缓存实现；构造时注册到
 * {@link ChatCache} 静态门面，替换其默认实现，行为与重构前保持一致。</p>
 */
@Service
public class ChatInfoCacheService implements ChatInfoStore {

    /** 缓存最大容量（会话数），超出后由 Caffeine 按策略淘汰。 */
    private static final int MAXIMUM_SIZE = 9999;

    /** 会话过期时间（分钟），写入后计时。 */
    private static final int EXPIRE_AFTER_WRITE_MINUTES = 30;

    private final Cache<String, ChatInfo> cache;

    public ChatInfoCacheService() {
        this(Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
                .build());
    }

    public ChatInfoCacheService(Cache<String, ChatInfo> cache) {
        this.cache = cache;
        ChatCache.init(this);
    }

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