package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：文件扩展名到 MIME 的映射流程。
 */
class MimeTypeUtilsTest {

    @Test
    void getMimeType_knownExtensions() {
        assertThat(MimeTypeUtils.getMimeType("jpg")).isEqualTo("image/jpeg");
        assertThat(MimeTypeUtils.getMimeType("png")).isEqualTo("image/png");
        assertThat(MimeTypeUtils.getMimeType("gif")).isEqualTo("image/gif");
    }

    @Test
    void getMimeType_isCaseInsensitive() {
        assertThat(MimeTypeUtils.getMimeType("JPEG")).isEqualTo("image/jpeg");
    }

    @Test
    void getMimeType_unknownExtensionReturnsDefault() {
        assertThat(MimeTypeUtils.getMimeType("docx")).isEqualTo("image/jpeg");
    }

    @Test
    void getMimeType_nullOrEmptyReturnsDefault() {
        assertThat(MimeTypeUtils.getMimeType(null)).isEqualTo("image/jpeg");
        assertThat(MimeTypeUtils.getMimeType("")).isEqualTo("image/jpeg");
    }
}