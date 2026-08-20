package com.maxkb4j.oss.support;

import com.maxkb4j.common.exception.FileLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试：上传校验器的白名单、文件名安全与空文件处理。
 */
class UploadValidatorTest {

    private final UploadValidator validator = new UploadValidator();

    @Test
    void allowsWhitelistedExtensions_caseInsensitive() {
        assertThatCode(() -> validator.validate(file("photo.png"))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(file("文档.PDF"))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(file("data.CSV"))).doesNotThrowAnyException();
    }

    @Test
    void rejectsDisallowedExtensions() {
        assertThatThrownBy(() -> validator.validate(file("app.exe"))).isInstanceOf(FileLimitExceededException.class);
        assertThatThrownBy(() -> validator.validate(file("shell.jsp"))).isInstanceOf(FileLimitExceededException.class);
        assertThatThrownBy(() -> validator.validate(file("script.js"))).isInstanceOf(FileLimitExceededException.class);
        assertThatThrownBy(() -> validator.validate(file("no-extension"))).isInstanceOf(FileLimitExceededException.class);
        assertThatThrownBy(() -> validator.validate(file("trailing-dot."))).isInstanceOf(FileLimitExceededException.class);
    }

    @Test
    void rejectsUnsafeFileNames() {
        assertThatThrownBy(() -> validator.validate(file("../etc/passwd.png"))).isInstanceOf(FileLimitExceededException.class);
        assertThatThrownBy(() -> validator.validate(file("dir/inner.png"))).isInstanceOf(FileLimitExceededException.class);
        assertThatThrownBy(() -> validator.validate(file("win\\path.png"))).isInstanceOf(FileLimitExceededException.class);
    }

    @Test
    void rejectsNullOrEmptyFiles() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(FileLimitExceededException.class);
        assertThatThrownBy(() -> validator.validate(
                new MockMultipartFile("file", "a.png", null, new byte[0])))
                .isInstanceOf(FileLimitExceededException.class);
        assertThatThrownBy(() -> validator.validate(
                new MockMultipartFile("file", "", null, "x".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(FileLimitExceededException.class);
    }

    private MultipartFile file(String name) {
        return new MockMultipartFile("file", name, null, "content".getBytes(StandardCharsets.UTF_8));
    }
}
