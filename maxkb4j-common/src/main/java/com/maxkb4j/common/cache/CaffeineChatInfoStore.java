package com.maxkb4j.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.domain.dto.ChatInfo;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 聊天会话缓存默认实现：Caffeine 本地缓存（Bean 化）。
 *
 * <p>chat / workflow / application 各模块通过构造函数注入 {@link ChatInfoStore} 使用；
 * 需要替换存储实现时提供新的 {@link ChatInfoStore} Bean，并用 {@code @Primary} / {@code @Qualifier}
 * 指定生效实现。</p>
 */
@Component
public class CaffeineChatInfoStore implements ChatInfoStore {

    /**
     * 缓存最大容量（会话数），超出后由 Caffeine 按策略淘汰。
     */
    private static final int MAXIMUM_SIZE = 9999;

    /**
     * 会话过期时间（分钟），写入后计时。
     */
    private static final int EXPIRE_AFTER_WRITE_MINUTES = 1440;

    private final Cache<String, ChatInfo> cache;

    public CaffeineChatInfoStore() {
        this(Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
                .build());
    }

    /**
     * 测试用构造器：注入缓存实例以隔离或断言缓存行为。
     */
    CaffeineChatInfoStore(Cache<String, ChatInfo> cache) {
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
