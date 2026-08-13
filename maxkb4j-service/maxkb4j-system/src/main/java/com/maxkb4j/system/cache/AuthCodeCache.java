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

    /** 缓存最大容量（邮件验证码条数）。 */
    private static final int MAXIMUM_SIZE = 9999;

    /** 验证码过期时间（分钟）。 */
    private static final int EXPIRE_MINUTES = 1;

    private final Cache<String, String> cache;

    public AuthCodeCache() {
        this(Caffeine.newBuilder()
                .initialCapacity(5)
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
                .expireAfterAccess(EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build());
    }

    public AuthCodeCache(Cache<String, String> cache) {
        this.cache = cache;
    }

    public void put(String email, String code) {
        cache.put(email, code);
    }

    public String getIfPresent(String email) {
        return cache.getIfPresent(email);
    }
}