package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：流读取与静默关闭流程。
 */
class IoUtilTest {

    /** 记录是否被关闭的 InputStream 包装。 */
    private static final class TrackingInputStream extends InputStream {
        private final ByteArrayInputStream delegate;
        boolean closed;

        TrackingInputStream(byte[] data) {
            this.delegate = new ByteArrayInputStream(data);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    void readToString_readsUtf8Content() {
        TrackingInputStream in = new TrackingInputStream("hello".getBytes(StandardCharsets.UTF_8));
        assertThat(IoUtil.readToString(in)).isEqualTo("hello");
        assertThat(in.closed).isTrue();
    }

    @Test
    void readToString_withExplicitCharset() {
        InputStream in = new ByteArrayInputStream("你好".getBytes(StandardCharsets.UTF_8));
        assertThat(IoUtil.readToString(in, StandardCharsets.UTF_8)).isEqualTo("你好");
    }

    @Test
    void readToString_emptyStreamReturnsEmpty() {
        assertThat(IoUtil.readToString(new ByteArrayInputStream(new byte[0]))).isEqualTo("");
    }

    @Test
    void readToString_swallowsIOExceptionAndReturnsEmpty() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };
        assertThat(IoUtil.readToString(failing)).isEqualTo("");
    }

    @Test
    void closeQuietly_flushesAndCloses() {
        RecordingCloseable c = new RecordingCloseable();
        IoUtil.closeQuietly(c);
        assertThat(c.flushed).isTrue();
        assertThat(c.closed).isTrue();
    }

    @Test
    void closeQuietly_nullIsNoOp() {
        IoUtil.closeQuietly(null);
    }

    @Test
    void closeQuietly_swallowsIOExceptionOnClose() {
        Closeable throwing = () -> {
            throw new IOException("close failed");
        };
        IoUtil.closeQuietly(throwing);
    }

    private static final class RecordingCloseable implements Closeable, Flushable {
        boolean flushed;
        boolean closed;

        @Override
        public void flush() {
            flushed = true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}