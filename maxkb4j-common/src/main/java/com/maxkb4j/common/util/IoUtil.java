package com.maxkb4j.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

@Slf4j
public class IoUtil extends StreamUtils {

    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            if (closeable instanceof Flushable) {
                try {
                    ((Flushable) closeable).flush();
                } catch (IOException var3) {
                    log.error(var3.getMessage(), var3);
                }
            }

            try {
                closeable.close();
            } catch (IOException var2) {
                log.error(var2.getMessage(), var2);
            }

        }
    }

    public static String readToString(InputStream input) {
        return readToString(input, Charsets.UTF_8);
    }

    public static String readToString(@Nullable InputStream input, Charset charset) {
        try {
            return copyToString(input, charset);
        } catch (IOException var6) {
            // 读取失败必须向上抛出（BUG-9）：吞掉异常会返回已读部分或空串，导致空段落静默入库、导入失败无感知
            throw new UncheckedIOException("读取输入流失败", var6);
        } finally {
            closeQuietly(input);
        }
    }
}
