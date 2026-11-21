package com.maxkb4j.common.cache;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.util.StpKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 无状态 JWT 撤销缓存校验逻辑测试。
 * <p>sa-token-jwt 的 eff 为失效时间(毫秒)而非签发时间，签发时间由 StpKit 写入的
 * iat claim 提供；无 iat 的令牌（升级前存量）按 fail-closed 拒绝。
 */
class UserTokenRevokeCacheTest {

    private static final String USER_ID = "user-1";
    private static final String SECRET = "test-secret-key-for-token-revoke";
    private static final long TOKEN_TIMEOUT_SECONDS = 86400;

    private SaTokenConfig originalAdminConfig;

    @BeforeEach
    void setUp() {
        originalAdminConfig = StpKit.ADMIN.getConfig();
    }

    @AfterEach
    void tearDown() {
        StpKit.ADMIN.setConfig(originalAdminConfig);
    }

    /**
     * 为 StpKit.ADMIN（签发时自动携带 iat）设置测试密钥。
     */
    private cn.dev33.satoken.jwt.StpLogicJwtForStateless adminLogic() {
        return (cn.dev33.satoken.jwt.StpLogicJwtForStateless) StpKit.ADMIN;
    }

    private void useTestSecretOnAdmin() {
        SaTokenConfig config = new SaTokenConfig();
        config.setJwtSecretKey(SECRET);
        config.setTimeout(TOKEN_TIMEOUT_SECONDS);
        StpKit.ADMIN.setConfig(config);
    }

    /**
     * 不经过 StpKit 的裸 StpLogic（签发令牌不带 iat，模拟升级前的存量令牌）。
     */
    private StpLogicJwtForStateless legacyStpLogic() {
        StpLogicJwtForStateless stpLogic = new StpLogicJwtForStateless(LoginType.ADMIN);
        SaTokenConfig config = new SaTokenConfig();
        config.setJwtSecretKey(SECRET);
        config.setTimeout(TOKEN_TIMEOUT_SECONDS);
        stpLogic.setConfig(config);
        return stpLogic;
    }

    private String adminToken() {
        return adminLogic().createTokenValue(USER_ID, "PC", TOKEN_TIMEOUT_SECONDS, null);
    }

    private Cache<String, Long> seededCache(long revokedAt) {
        Cache<String, Long> cache = Caffeine.newBuilder().build();
        cache.put(USER_ID, revokedAt);
        return cache;
    }

    @Test
    void noRevocationRecord_tokenPasses() {
        UserTokenRevokeCache cache = new UserTokenRevokeCache();
        useTestSecretOnAdmin();

        assertThatCode(() -> cache.assertNotRevoked(USER_ID, adminToken(), adminLogic()))
                .doesNotThrowAnyException();
    }

    @Test
    void tokenIssuedAfterRevocation_passes() {
        UserTokenRevokeCache cache = new UserTokenRevokeCache();
        useTestSecretOnAdmin();
        cache.revoke(USER_ID);
        // 撤销后重新登录签发的新令牌 iat >= 撤销时间戳，应放行
        String freshToken = adminToken();

        assertThatCode(() -> cache.assertNotRevoked(USER_ID, freshToken, adminLogic()))
                .doesNotThrowAnyException();
    }

    @Test
    void tokenIssuedBeforeRevocation_rejected() throws InterruptedException {
        UserTokenRevokeCache cache = new UserTokenRevokeCache();
        useTestSecretOnAdmin();
        String oldToken = adminToken();
        // 等待跨越秒级边界，保证撤销时间戳严格大于令牌签发时间
        TimeUnit.MILLISECONDS.sleep(1100);
        cache.revoke(USER_ID);

        assertThatThrownBy(() -> cache.assertNotRevoked(USER_ID, oldToken, adminLogic()))
                .isInstanceOf(NotLoginException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(((NotLoginException) e).getType())
                        .isEqualTo(NotLoginException.TOKEN_FREEZE));
    }

    @Test
    void seededFutureRevocation_rejectsOldTokenWithoutSleep() {
        useTestSecretOnAdmin();
        String oldToken = adminToken();
        long now = System.currentTimeMillis() / 1000;
        UserTokenRevokeCache cache = new UserTokenRevokeCache(seededCache(now + 60));

        assertThatThrownBy(() -> cache.assertNotRevoked(USER_ID, oldToken, adminLogic()))
                .isInstanceOf(NotLoginException.class);
    }

    @Test
    void tokenWithoutIatClaim_rejectedWhenRevoked_failClosed() {
        StpLogicJwtForStateless legacy = legacyStpLogic();
        long now = System.currentTimeMillis() / 1000;
        UserTokenRevokeCache cache = new UserTokenRevokeCache(seededCache(now + 60));
        // 裸 StpLogic 签发的令牌不含 iat，存在撤销记录时按 fail-closed 拒绝
        String legacyToken = legacy.createTokenValue(USER_ID, "PC", TOKEN_TIMEOUT_SECONDS, null);

        assertThatThrownBy(() -> cache.assertNotRevoked(USER_ID, legacyToken, legacy))
                .isInstanceOf(NotLoginException.class);
    }

    @Test
    void unparseableToken_rejectedWhenRevoked_failClosed() {
        long now = System.currentTimeMillis() / 1000;
        UserTokenRevokeCache cache = new UserTokenRevokeCache(seededCache(now + 60));

        assertThatThrownBy(() -> cache.assertNotRevoked(USER_ID, "not-a-jwt", adminLogic()))
                .isInstanceOf(NotLoginException.class);
    }

    @Test
    void revokeAll_coversEveryUser() {
        UserTokenRevokeCache cache = new UserTokenRevokeCache();
        useTestSecretOnAdmin();
        cache.revokeAll(List.of(USER_ID, "user-2"));
        String freshToken = adminToken();

        assertThatCode(() -> cache.assertNotRevoked(USER_ID, freshToken, adminLogic()))
                .doesNotThrowAnyException();
        // 未登录用户无撤销记录也不受影响
        assertThatCode(() -> cache.assertNotRevoked("user-3", adminToken(), adminLogic()))
                .doesNotThrowAnyException();
    }

    @Test
    void revokeAllWithEmptyList_noop() {
        UserTokenRevokeCache cache = new UserTokenRevokeCache();
        cache.revokeAll(List.of());
        cache.revokeAll(null);
        useTestSecretOnAdmin();

        assertThatCode(() -> cache.assertNotRevoked(USER_ID, adminToken(), adminLogic()))
                .doesNotThrowAnyException();
    }
}
