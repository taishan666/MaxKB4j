package com.maxkb4j.knowledge.parser.impl;

import com.maxkb4j.knowledge.parser.DocumentParser;
import com.maxkb4j.knowledge.util.EncodingDetectUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.io.TikaInputStream;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class TxtParser implements DocumentParser {

    @Override
    public List<String> getExtensions() {
        return List.of(".txt");
    }


    @Override
    public String handle(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try (TikaInputStream tikaStream = TikaInputStream.get(inputStream)) {
            // 检测字符编码（检测失败回退 UTF-8），避免 GBK 等编码按 UTF-8 解码产生乱码
            Charset charset = EncodingDetectUtil.detectAndReset(tikaStream);
            TextDocumentParser parser = new TextDocumentParser(charset);
            Document document = parser.parse(tikaStream);
            return EncodingDetectUtil.stripBom(document.text());
        } catch (Exception e) {
            log.error("解析 TXT 文件失败", e);
            throw new RuntimeException("TXT 解析异常: " + e.getMessage(), e);
        }
    }
}
