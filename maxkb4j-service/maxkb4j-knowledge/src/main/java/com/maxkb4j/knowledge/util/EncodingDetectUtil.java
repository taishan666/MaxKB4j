package com.maxkb4j.knowledge.util;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.txt.UniversalEncodingDetector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 文本编码检测工具，基于 Tika UniversalEncodingDetector（支持 BOM 与 GBK/GB18030 等常见编码），
 * 检测失败时回退为 UTF-8，避免 txt/md 等文本文件因固定 UTF-8 解码导致乱码入库。
 */
public class EncodingDetectUtil {

    private EncodingDetectUtil() {
    }

    /**
     * 检测流的字符编码并重置流（detect 可能已消费部分内容），供后续按检测到的编码读取。
     * 入参须为 TikaInputStream（始终支持 mark/reset），由调用方负责关闭。
     */
    public static Charset detectAndReset(TikaInputStream stream) {
        Charset charset = detect(stream);
        try {
            stream.reset();
        } catch (IOException e) {
            // 理论上不会发生：TikaInputStream 始终支持 mark/reset
        }
        return charset;
    }

    /**
     * 检测流的字符编码，检测失败时回退为 UTF-8。
     */
    public static Charset detect(TikaInputStream stream) {
        try {
            Charset charset = new UniversalEncodingDetector().detect(stream, new Metadata());
            return charset != null ? charset : StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 检测字节数组的字符编码，检测失败时回退为 UTF-8。
     */
    public static Charset detect(byte[] bytes) {
        try (TikaInputStream stream = TikaInputStream.get(bytes)) {
            return detect(stream);
        } catch (IOException e) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 去除文本开头的 BOM 字符（\uFEFF）。UTF-16 及带 BOM 的 UTF-8 按具体编码解码后会保留该字符。
     */
    public static String stripBom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }
}
