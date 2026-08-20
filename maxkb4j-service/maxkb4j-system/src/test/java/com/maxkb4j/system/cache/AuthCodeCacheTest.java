package com.maxkb4j.system.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证码缓存 Bean 行为测试（第 3 期组件化）。
 */
class AuthCodeCacheTest {

    @Test
    void putAndGetIfPresent_roundTrip() {
        AuthCodeCache cache = new AuthCodeCache();
        cache.put("user@example.com", "123456");
        assertThat(cache.getIfPresent("user@example.com")).isEqualTo("123456");
    }

    @Test
    void getIfPresent_unknownEmailReturnsNull() {
        AuthCodeCache cache = new AuthCodeCache();
        assertThat(cache.getIfPresent("nobody@example.com")).isNull();
    }
}