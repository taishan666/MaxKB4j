package com.maxkb4j.system.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮件验证码缓存（第 3 期 Bean 化，原为 common 静态类）。
 * <p>构造器注入 Caffeine 实例，测试可替换缓存实现或断言过期行为。</p>
 */
@Service
public class AuthCodeCache {

    /**
     * 缓存最大容量（邮件验证码条数）。
     */
    private static final int MAXIMUM_SIZE = 9999;

    /**
     * 验证码过期时间（分钟）。
     */
    private static final int EXPIRE_MINUTES = 5;

    /**
     * 发送间隔（秒），防止无限发送验证码轰炸。
     */
    private static final int SEND_INTERVAL_SECONDS = 60;

    private final Cache<String, String> cache;
    private final Cache<String, Long> sendTimestampCache;

    public AuthCodeCache() {
        this(Caffeine.newBuilder()
                .initialCapacity(5)
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
                .expireAfterAccess(EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build(),
                Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build());
    }

    public AuthCodeCache(Cache<String, String> cache, Cache<String, Long> sendTimestampCache) {
        this.cache = cache;
        this.sendTimestampCache = sendTimestampCache;
    }

    public void put(String email, String code) {
        cache.put(email, code);
    }

    public String getIfPresent(String email) {
        return cache.getIfPresent(email);
    }

    /**
     * 检查是否可以发送验证码（防止无限轰炸）。
     *
     * @param email 邮箱
     * @return true 表示可以发送，false 表示发送过于频繁
     */
    public boolean canSend(String email) {
        Long lastSend = sendTimestampCache.getIfPresent(email);
        if (lastSend != null) {
            long elapsed = (System.currentTimeMillis() - lastSend) / 1000;
            if (elapsed < SEND_INTERVAL_SECONDS) {
                return false;
            }
        }
        return true;
    }

    /**
     * 记录发送时间戳。
     */
    public void recordSend(String email) {
        sendTimestampCache.put(email, System.currentTimeMillis());
    }

    /**
     * 验证成功后使验证码失效，防止重放攻击。
     */
    public void invalidate(String email) {
        cache.invalidate(email);
    }
}