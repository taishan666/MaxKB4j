package com.maxkb4j.knowledge.parser.impl;

import com.alibaba.excel.EasyExcel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelParserTest {

    private final ExcelParser parser = new ExcelParser();

    private byte[] writeWorkbook(List<List<String>> head, List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).head(head).sheet().doWrite(rows);
        return out.toByteArray();
    }

    @Test
    void shouldFillBlankHeaderCellWithPlaceholder() {
        // 首列表头为空 → 读取时 headMap 缺键或值为 null/空串，修复前会漏列或产生 null 表头导致 String.join NPE
        byte[] bytes = writeWorkbook(
                List.of(List.of(""), List.of("名称"), List.of("数量")),
                List.of(List.of("值A", "值B", "值C")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        assertTrue(markdown.startsWith("| 列1 | 名称 | 数量 |\n"), markdown);
        assertTrue(markdown.contains("| 值A| 值B| 值C |"), markdown);
        assertFalse(markdown.contains("null"), markdown);
    }

    @Test
    void shouldFillWhitespaceHeaderCellWithPlaceholder() {
        byte[] bytes = writeWorkbook(
                List.of(List.of("名称"), List.of("  "), List.of("数量")),
                List.of(List.of("值A", "值B", "值C")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        assertTrue(markdown.startsWith("| 名称 | 列2 | 数量 |\n"), markdown);
        assertFalse(markdown.contains("null"), markdown);
    }

    @Test
    void shouldPadShortDataRowAndKeepColumnAlignment() {
        // 数据行列数少于表头时补空串，保证 markdown 列与表头对齐
        byte[] bytes = writeWorkbook(
                List.of(List.of("名称"), List.of("数量"), List.of("备注")),
                List.of(List.of("值A", "值B")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        assertTrue(markdown.contains("| 值A| 值B|  |"), markdown);
    }
}
