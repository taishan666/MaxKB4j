package com.maxkb4j.system.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.exception.LoginException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录失败限流器：按「用户名 + 来源 IP」计数，窗口内失败超过阈值即锁定一段时间。
 * <p>内存实现，多实例部署时各节点独立计数（防护效果减弱但不影响正确性），
 * 集群化部署时应替换为 Redis 等共享存储。</p>
 *
 * @author tarzan
 */
@Component
public class LoginAttemptLimiter {

    /** 锁定前允许的最大连续失败次数。 */
    private static final int MAX_ATTEMPTS = 5;

    /** 锁定时长（分钟）。 */
    private static final int LOCK_MINUTES = 15;

    /** 失败计数窗口（分钟），窗口内累计失败次数。 */
    private static final int ATTEMPT_WINDOW_MINUTES = 15;

    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(ATTEMPT_WINDOW_MINUTES, TimeUnit.MINUTES)
            .build();

    private final Cache<String, Boolean> lockouts = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(LOCK_MINUTES, TimeUnit.MINUTES)
            .build();

    /**
     * 校验是否处于锁定状态，锁定中则抛出登录异常。
     *
     * @param key 限流键（用户名 + 来源 IP）
     */
    public void checkLocked(String key) {
        if (key != null && lockouts.getIfPresent(key) != null) {
            throw new LoginException("login.too.many.attempts");
        }
    }

    /**
     * 记录一次登录失败，达到阈值时触发锁定。
     */
    public void recordFailure(String key) {
        if (key == null) {
            return;
        }
        int count = attempts.get(key, k -> new AtomicInteger(0)).incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            lockouts.put(key, Boolean.TRUE);
            attempts.invalidate(key);
        }
    }

    /**
     * 登录成功后清除失败计数。
     */
    public void recordSuccess(String key) {
        if (key != null) {
            attempts.invalidate(key);
        }
    }
}
