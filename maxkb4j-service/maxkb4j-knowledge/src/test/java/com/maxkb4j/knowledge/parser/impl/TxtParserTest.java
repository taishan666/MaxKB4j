package com.maxkb4j.knowledge.parser.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TxtParserTest {

    private final TxtParser parser = new TxtParser();

    private static final String TEXT = "知识库文档解析测试：这是一段中文内容，用于验证字符编码检测。\n"
            + "第二行中文内容，确保 GBK 编码的文本不会被按 UTF-8 解码成乱码入库并向量化。\n"
            + "第三行补充英文 mixed content 与数字 12345，重复一遍：知识库文档解析测试。";

    @Test
    void shouldParseGbkText() {
        byte[] bytes = TEXT.getBytes(Charset.forName("GBK"));
        assertEquals(TEXT, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldParseUtf8Text() {
        byte[] bytes = TEXT.getBytes(StandardCharsets.UTF_8);
        assertEquals(TEXT, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldParseUtf8BomText() {
        byte[] bytes = ('\uFEFF' + TEXT).getBytes(StandardCharsets.UTF_8);
        assertEquals(TEXT, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldParseUtf16LeBomText() {
        byte[] bytes = ('\uFEFF' + TEXT).getBytes(StandardCharsets.UTF_16LE);
        assertEquals(TEXT, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldParseAsciiText() {
        String text = "plain ascii content 123";
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        assertEquals(text, parser.handle(new ByteArrayInputStream(bytes)));
    }

    @Test
    void shouldReturnEmptyForNullStream() {
        assertEquals("", parser.handle(null));
    }

    @Test
    void shouldThrowForEmptyStream() {
        // langchain4j TextDocumentParser 对空白文档抛出异常，与修复前行为一致
        assertThrows(RuntimeException.class, () -> parser.handle(new ByteArrayInputStream(new byte[0])));
    }
}
