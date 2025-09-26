package com.tarzan.maxkb4j.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@Slf4j
public class IoUtil extends StreamUtils {

    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            if (closeable instanceof Flushable) {
                try {
                    ((Flushable)closeable).flush();
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
        String var8 = "";
        try {
            var8 = copyToString(input, charset);
        } catch (IOException var6) {
            log.error(var6.getMessage(), var6);
        } finally {
            closeQuietly(input);
        }
        return var8;
    }
}
