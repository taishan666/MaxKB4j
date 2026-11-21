package com.maxkb4j.knowledge.parser.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MDParserTest {

    private final MDParser parser = new MDParser();

    private static final String TEXT = "# 知识库文档解析测试\n\n"
            + "这是一段**中文 Markdown** 内容，用于验证字符编码检测。\n"
            + "- 列表项一：确保 GBK 编码的文本不会被按 UTF-8 解码成乱码入库并向量化\n"
            + "- 列表项二：mixed content 与数字 12345，重复一遍：知识库文档解析测试";

    @Test
    void shouldParseGbkMarkdown() {
        byte[] bytes = TEXT.getBytes(Charset.forName("GBK"));
        assertEquals(TEXT, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldParseUtf8Markdown() {
        byte[] bytes = TEXT.getBytes(StandardCharsets.UTF_8);
        assertEquals(TEXT, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldParseUtf8BomMarkdown() {
        byte[] bytes = ('\uFEFF' + TEXT).getBytes(StandardCharsets.UTF_8);
        assertEquals(TEXT, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldParseUtf16LeBomMarkdown() {
        byte[] bytes = ('\uFEFF' + TEXT).getBytes(StandardCharsets.UTF_16LE);
        assertEquals(TEXT, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldReturnEmptyForNullStream() {
        assertEquals("", parser.handle(null));
    }

    @Test
    void shouldReturnEmptyForEmptyStream() {
        assertEquals("", parser.handle(new ByteArrayInputStream(new byte[0])));
    }
}
