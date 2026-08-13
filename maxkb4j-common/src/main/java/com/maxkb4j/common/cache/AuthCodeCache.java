package com.maxkb4j.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class AuthCodeCache {

    /** 缓存最大容量（邮件验证码条数）。 */
    private static final int MAXIMUM_SIZE = 9999;

    /** 验证码过期时间（分钟）。 */
    private static final int EXPIRE_MINUTES = 1;

    // 创建缓存并配置
    private static final Cache<String, String> AUTH_CODE_CACHE = Caffeine.newBuilder()
            .initialCapacity(5)
            // 超出最大容量时淘汰
            .maximumSize(MAXIMUM_SIZE)
            //设置写缓存后n秒钟过期
            .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
            .expireAfterAccess(EXPIRE_MINUTES, TimeUnit.MINUTES)
            .build();

    public static void put(String email, String code) {
        AUTH_CODE_CACHE.put(email, code);
    }

    public static String getIfPresent(String email) {
        return AUTH_CODE_CACHE.getIfPresent(email);
    }
}
