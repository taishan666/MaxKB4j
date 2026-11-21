package com.maxkb4j.common.cache;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.jwt.SaJwtUtil;
import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.hutool.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.StpKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 用户令牌撤销缓存（无状态 JWT 的服务端失效兜底）。
 *
 * <p>Sa-Token 的 {@code StpLogicJwtForStateless} 模式下令牌自包含、服务端不存会话，
 * {@code logout()} 等操作无法使已签发的令牌失效。本缓存按用户记录「撤销时间戳」，
 * 请求侧比对令牌签发时间({@link StpKit#ISSUED_AT_CLAIM} iat claim)：签发时间早于撤销
 * 时间戳的令牌一律拒绝。禁用用户、删除用户、重置口令后调用 {@link #revoke(String)}
 * 即可令其存量令牌立即失效；撤销后的重新登录会获得携带新 iat 的令牌，不受影响。
 *
 * <p>撤销记录的过期时间取 sa-token 的 token 有效期(timeout) 加缓冲，
 * 保证撤销记录存活期间覆盖所有存量令牌的自然过期；令牌配置为永不过期(timeout=-1)时
 * 撤销记录不过期，由缓存容量上限兜底淘汰。
 *
 * @author tarzan
 */
@Slf4j
@Component
public class UserTokenRevokeCache {

    /**
     * 缓存最大容量（用户数），超出按访问新旧淘汰，防止长期未过期令牌场景下无限增长。
     */
    private static final long MAXIMUM_SIZE = 100_000;

    /**
     * token 有效期之外的过期缓冲（秒），防止撤销记录与令牌同时到期造成的边界泄漏。
     */
    private static final long EXPIRE_BUFFER_SECONDS = TimeUnit.HOURS.toSeconds(1);

    /**
     * 无法读取 sa-token 配置时的默认 token 有效期（秒）。
     */
    private static final long DEFAULT_TOKEN_TIMEOUT_SECONDS = TimeUnit.DAYS.toSeconds(1);

    private final Cache<String, Long> cache;

    public UserTokenRevokeCache() {
        this(Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfter(new RevokedAtExpiry())
                .build());
    }

    UserTokenRevokeCache(Cache<String, Long> cache) {
        this.cache = cache;
    }

    /**
     * 撤销指定用户的全部存量令牌（记录当前撤销时间戳）。
     * 撤销后签发于该时间点之前的令牌在请求侧校验时即被拒绝。
     */
    public void revoke(String userId) {
        cache.put(userId, System.currentTimeMillis() / 1000);
    }

    /**
     * 批量撤销多个用户的存量令牌。
     */
    public void revokeAll(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        long revokedAt = System.currentTimeMillis() / 1000;
        userIds.forEach(userId -> cache.put(userId, revokedAt));
    }

    /**
     * 校验令牌未被撤销：签发时间(iat)早于撤销时间戳时抛出 TOKEN_FREEZE 未登录异常。
     * <p>失败安全（fail-closed）：令牌缺少 iat（如升级前的存量令牌）或解析失败时，
     * 只要该用户存在撤销记录即拒绝——客户端重新登录即可获得携带 iat 的新令牌。
     *
     * @param userId     已解析的登录用户 ID
     * @param tokenValue 当前请求令牌
     * @param stpLogic   令牌所属的 StpLogic（用于取 loginType 与签名密钥）
     */
    public void assertNotRevoked(String userId, String tokenValue, StpLogicJwtForStateless stpLogic) {
        Long revokedAt = cache.getIfPresent(userId);
        if (revokedAt == null) {
            return;
        }
        long issuedAt = tokenIssuedAt(tokenValue, stpLogic);
        if (issuedAt <= 0 || issuedAt < revokedAt) {
            throw new NotLoginException(I18nUtil.get("auth.token.invalid"), stpLogic.getLoginType(), NotLoginException.TOKEN_FREEZE);
        }
    }

    /**
     * 解析令牌签发时间(iat，秒级时间戳)；解析失败返回 0。
     */
    private long tokenIssuedAt(String tokenValue, StpLogicJwtForStateless stpLogic) {
        try {
            JSONObject payloads = SaJwtUtil.getPayloads(tokenValue, stpLogic.getLoginType(), stpLogic.jwtSecretKey());
            return payloads.getLong(StpKit.ISSUED_AT_CLAIM, 0L);
        } catch (Exception e) {
            log.debug("解析令牌签发时间失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 撤销记录过期策略：写入/更新时按 token 有效期+缓冲重新计时，读取不续期。
     */
    private static final class RevokedAtExpiry implements Expiry<String, Long> {

        @Override
        public long expireAfterCreate(String key, Long revokedAt, long now) {
            return tokenTimeoutNanos();
        }

        @Override
        public long expireAfterUpdate(String key, Long revokedAt, long now, long currentDuration) {
            return tokenTimeoutNanos();
        }

        @Override
        public long expireAfterRead(String key, Long revokedAt, long now, long currentDuration) {
            return currentDuration;
        }
    }

    /**
     * 撤销记录过期时长（纳秒）= sa-token timeout + 缓冲；timeout 为 -1（永不过期）时返回永不过期。
     */
    private static long tokenTimeoutNanos() {
        long timeout = DEFAULT_TOKEN_TIMEOUT_SECONDS;
        try {
            timeout = SaManager.getConfig().getTimeout();
        } catch (Exception e) {
            log.debug("读取 sa-token timeout 配置失败，使用默认值 {}s: {}", DEFAULT_TOKEN_TIMEOUT_SECONDS, e.getMessage());
        }
        if (timeout < 0) {
            return Long.MAX_VALUE;
        }
        return TimeUnit.NANOSECONDS.convert(timeout + EXPIRE_BUFFER_SECONDS, TimeUnit.SECONDS);
    }
}
