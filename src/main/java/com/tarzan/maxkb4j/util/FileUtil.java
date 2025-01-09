package com.tarzan.maxkb4j.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

@Slf4j
public class FileUtil extends FileCopyUtils {

    public static String readToString(final File file) {
        return readToString(file, Charsets.UTF_8);
    }

    public static String readToString(final File file, final Charset encoding) {
        try {
            InputStream in = Files.newInputStream(file.toPath());
            Throwable var3 = null;

            String var4;
            try {
                var4 = IoUtil.readToString(in, encoding);
            } catch (Throwable var14) {
                var3 = var14;
                throw var14;
            } finally {
                if (var3 != null) {
                    try {
                        in.close();
                    } catch (Throwable var13) {
                        var3.addSuppressed(var13);
                    }
                } else {
                    in.close();
                }

            }

            return var4;
        } catch (IOException var16) {
            log.error(var16.getMessage(), var16);
        }
        return "";
    }

}
