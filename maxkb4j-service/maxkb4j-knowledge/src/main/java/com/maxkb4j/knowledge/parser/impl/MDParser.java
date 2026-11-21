package com.maxkb4j.knowledge.parser.impl;

import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.knowledge.parser.DocumentParser;
import com.maxkb4j.knowledge.util.EncodingDetectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.io.TikaInputStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class MDParser implements DocumentParser {

    @Override
    public List<String> getExtensions() {
        return List.of(".md");
    }

    @Override
    public String handle(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try (TikaInputStream tikaStream = TikaInputStream.get(inputStream)) {
            // 检测字符编码（检测失败回退 UTF-8），避免 GBK 等编码按 UTF-8 解码产生乱码
            Charset charset = EncodingDetectUtil.detectAndReset(tikaStream);
            return EncodingDetectUtil.stripBom(IoUtil.readToString(tikaStream, charset));
        } catch (IOException e) {
            log.error("解析 MD 文件失败", e);
            throw new RuntimeException("MD 解析异常: " + e.getMessage(), e);
        }
    }
}
