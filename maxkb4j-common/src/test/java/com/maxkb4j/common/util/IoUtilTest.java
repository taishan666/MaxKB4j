package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试：readToString 读取失败时必须向上抛出（BUG-9），
 * 不得吞掉 IOException 返回已读部分或空串导致数据静默丢失（空段落入库、导入失败无感知）。
 */
class IoUtilTest {

    @Test
    void success_returnsFullContentAndClosesStream() {
        CloseTrackingStream stream = new CloseTrackingStream(
                new ByteArrayInputStream("你好 MaxKB4j".getBytes(StandardCharsets.UTF_8)));
        assertThat(IoUtil.readToString(stream, StandardCharsets.UTF_8)).isEqualTo("你好 MaxKB4j");
        assertThat(stream.closed).isTrue();
    }

    @Test
    void readFailure_throwsUncheckedIOExceptionAndClosesStream() throws IOException {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("模拟底层读取失败");
            }
        };
        CloseTrackingStream stream = new CloseTrackingStream(failing);
        assertThatThrownBy(() -> IoUtil.readToString(stream, StandardCharsets.UTF_8))
                .isInstanceOf(UncheckedIOException.class)
                .cause()
                .isInstanceOf(IOException.class)
                .hasMessageContaining("模拟底层读取失败");
        assertThat(stream.closed).isTrue();
    }

    @Test
    void partialReadThenFailure_throwsInsteadOfReturningPartialContent() throws IOException {
        // 先返回部分字节再失败：旧实现会把已读部分当成功结果返回，造成数据静默截断
        InputStream partialThenFail = new InputStream() {
            private boolean failed;

            @Override
            public int read() throws IOException {
                if (!failed) {
                    failed = true;
                    return 'P';
                }
                throw new IOException("中途读取失败");
            }
        };
        CloseTrackingStream stream = new CloseTrackingStream(partialThenFail);
        assertThatThrownBy(() -> IoUtil.readToString(stream, StandardCharsets.UTF_8))
                .isInstanceOf(UncheckedIOException.class);
        assertThat(stream.closed).isTrue();
    }

    private static class CloseTrackingStream extends FilterInputStream {
        boolean closed;

        CloseTrackingStream(InputStream delegate) {
            super(delegate);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
