package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：敏感数据脱敏流程。
 */
class DataMaskUtilTest {

    @Test
    void maskMobile_masksMiddleFourDigits() {
        assertThat(DataMaskUtil.maskMobile("13812341234")).isEqualTo("138****1234");
    }

    @Test
    void maskMobile_keepsInvalidLengthAndNullAsIs() {
        assertThat(DataMaskUtil.maskMobile(null)).isNull();
        assertThat(DataMaskUtil.maskMobile("123")).isEqualTo("123");
        assertThat(DataMaskUtil.maskMobile("1234567890")).isEqualTo("1234567890");
    }

    @Test
    void maskIdCard_keepsFirstThreeAndLastFour() {
        assertThat(DataMaskUtil.maskIdCard("110101199003078888")).isEqualTo("110****8888");
    }

    @Test
    void maskIdCard_keepsShortAndNullAsIs() {
        assertThat(DataMaskUtil.maskIdCard(null)).isNull();
        assertThat(DataMaskUtil.maskIdCard("12345")).isEqualTo("12345");
    }

    @Test
    void maskEmail_masksLongUsername() {
        assertThat(DataMaskUtil.maskEmail("tarzan@example.com")).isEqualTo("ta****n@example.com");
    }

    @Test
    void maskEmail_collapsesShortUsername() {
        assertThat(DataMaskUtil.maskEmail("ab@example.com")).isEqualTo("*@example.com");
    }

    @Test
    void maskEmail_keepsNullAndMissingAtAsIs() {
        assertThat(DataMaskUtil.maskEmail(null)).isNull();
        assertThat(DataMaskUtil.maskEmail("noemail")).isEqualTo("noemail");
    }

    @Test
    void maskBankCard_keepsFirstSixAndLastFour() {
        assertThat(DataMaskUtil.maskBankCard("6222021234567890123")).isEqualTo("622202****0123");
    }

    @Test
    void maskBankCard_keepsShortAndNullAsIs() {
        assertThat(DataMaskUtil.maskBankCard(null)).isNull();
        assertThat(DataMaskUtil.maskBankCard("1234")).isEqualTo("1234");
    }

    @Test
    void maskApiKey_keepsPrefixAndSuffix() {
        String key = "sk-abcd1234wxyz9876";
        String masked = DataMaskUtil.maskApiKey(key);
        assertThat(masked).startsWith("sk-a").endsWith("9876");
        assertThat(masked).hasSize(key.length());
        assertThat(masked).contains("*");
    }

    @Test
    void maskString_keepsShortStringUnmaskedByStarsForDots() {
        assertThat(DataMaskUtil.maskString(null, 4, 4)).isNull();
        // length (2) <= prefix+suffix (8): returns original dots replaced (none) -> unchanged
        assertThat(DataMaskUtil.maskString("ab", 2, 2)).isEqualTo("ab");
    }

    @Test
    void maskString_masksMiddleForLongString() {
        assertThat(DataMaskUtil.maskString("abcdefghij", 3, 2)).isEqualTo("abc*****ij");
    }
}