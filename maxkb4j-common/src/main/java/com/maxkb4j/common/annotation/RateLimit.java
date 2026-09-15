package com.maxkb4j.common.annotation;

import com.maxkb4j.common.enums.RateLimitKeyType;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * <p>
 * 基于固定窗口计数器算法，使用 Caffeine 本地缓存实现（单机限流）。
 * 集群部署时需替换为 Redis 实现。
 * <p>
 * 使用示例：
 * <pre>
 * // 每个 IP 60 秒内最多请求 100 次
 * {@code @RateLimit(limit = 100, window = 60)}
 *
 * // 每个登录用户 10 秒内最多请求 5 次
 * {@code @RateLimit(limit = 5, window = 10, keyType = RateLimitKeyType.USER)}
 *
 * // 自定义 SpEL key，按 userId 参数限流
 * {@code @RateLimit(limit = 10, window = 60, keyType = RateLimitKeyType.CUSTOM, key = "#userId")}
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求次数，默认 100
     */
    int limit() default 100;

    /**
     * 时间窗口大小（秒），默认 60
     */
    int window() default 60;

    /**
     * 限流维度，默认按 IP
     */
    RateLimitKeyType keyType() default RateLimitKeyType.IP;

    /**
     * 自定义限流 key（SpEL 表达式），仅 keyType = CUSTOM 时生效。
     * 可通过 #paramName 引用方法参数，例如 "#userId"
     */
    String key() default "";

    /**
     * 触发限流时的提示消息，为空则使用默认消息
     */
    String message() default "";
}
