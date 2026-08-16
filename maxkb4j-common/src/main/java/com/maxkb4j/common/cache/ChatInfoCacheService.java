package com.maxkb4j.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.domain.dto.ChatInfo;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 聊天会话缓存 Spring Bean 实现（第 3 期完成态）。
 *
 * <p>作为统一缓存组件供 chat / workflow / application 各模块注入使用；
 * 默认构造器内置 Caffeine 本地缓存，也可通过构造器传入自定义
 * {@link Cache} 以便测试替换缓存实现。</p>
 */
@Service
public class ChatInfoCacheService implements ChatInfoStore {

    /** 缓存最大容量（会话数），超出后由 Caffeine 按策略淘汰。 */
    private static final int MAXIMUM_SIZE = 9999;

    /** 会话过期时间（分钟），写入后计时。 */
    private static final int EXPIRE_AFTER_WRITE_MINUTES = 1440;

    private final Cache<String, ChatInfo> cache;

    public ChatInfoCacheService() {
        this(Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
                .build());
    }

    public ChatInfoCacheService(Cache<String, ChatInfo> cache) {
        this.cache = cache;
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
