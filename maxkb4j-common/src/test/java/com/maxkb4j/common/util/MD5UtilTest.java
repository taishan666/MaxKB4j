package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：MD5 摘要流程。
 */
class MD5UtilTest {

    @Test
    void encrypt_matchesKnownDigest() {
        // MD5("hello") 标准值
        assertThat(MD5Util.encrypt("hello")).isEqualTo("5d41402abc4b2a76b9719d911017c592");
    }

    @Test
    void encrypt_isDeterministic() {
        assertThat(MD5Util.encrypt("MaxKB4j")).isEqualTo(MD5Util.encrypt("MaxKB4j"));
    }

    @Test
    void encrypt_differsForDifferentInput() {
        assertThat(MD5Util.encrypt("a")).isNotEqualTo(MD5Util.encrypt("b"));
    }

    @Test
    void encrypt_withRange_returnsSubstring() {
        assertThat(MD5Util.encrypt("hello", 0, 4)).isEqualTo("5d41");
    }
}