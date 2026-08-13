package com.maxkb4j.system.security;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：口令哈希服务的 BCrypt 写入与双格式（BCrypt / 旧版 MD5）校验。
 */
class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void encode_producesBcryptHash() {
        String hash = passwordService.encode("secret123");
        assertThat(hash).startsWith("$2");
        assertThat(passwordService.isLegacyHash(hash)).isFalse();
    }

    @Test
    void encode_isSalted_resultsDiffer() {
        assertThat(passwordService.encode("secret123"))
                .isNotEqualTo(passwordService.encode("secret123"));
    }

    @Test
    void matches_bcryptHash() {
        String hash = passwordService.encode("secret123");
        assertThat(passwordService.matches("secret123", hash)).isTrue();
        assertThat(passwordService.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void matches_legacyMd5Hash() {
        String legacy = SaSecureUtil.md5("secret123");
        assertThat(passwordService.isLegacyHash(legacy)).isTrue();
        assertThat(passwordService.matches("secret123", legacy)).isTrue();
        assertThat(passwordService.matches("wrong-password", legacy)).isFalse();
    }

    @Test
    void matches_nullAndBlankStoredHashNeverMatch() {
        assertThat(passwordService.matches("secret", null)).isFalse();
        assertThat(passwordService.matches("secret", "")).isFalse();
        assertThat(passwordService.matches(null, passwordService.encode("x"))).isFalse();
    }
}
