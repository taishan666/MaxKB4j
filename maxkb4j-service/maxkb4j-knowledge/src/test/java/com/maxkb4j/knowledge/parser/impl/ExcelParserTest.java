package com.maxkb4j.knowledge.parser.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
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

    private byte[] writeTwoSheetWorkbook(List<List<String>> head1, List<List<String>> rows1,
                                         List<List<String>> head2, List<List<String>> rows2) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ExcelWriter writer = EasyExcel.write(out).build()) {
            WriteSheet sheet1 = EasyExcel.writerSheet(0, "表一").head(head1).build();
            writer.write(rows1, sheet1);
            WriteSheet sheet2 = EasyExcel.writerSheet(1, "表二").head(head2).build();
            writer.write(rows2, sheet2);
        }
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

    @Test
    void shouldParseEachSheetIntoSeparateTable() {
        // 修复前：headers 跨 sheet 累积，第二个 sheet 的表头被追加到第一个 sheet 表头之后，
        // 生成“名称 | 数量 | 库存 | 产地”的重复表头，且 sheet1 行数据被错乱补齐
        byte[] bytes = writeTwoSheetWorkbook(
                List.of(List.of("名称"), List.of("数量")),
                List.of(List.of("值A", "值B")),
                List.of(List.of("库存"), List.of("产地")),
                List.of(List.of("值C", "值D")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        // 每个 sheet 独立成表，各自表头完整
        assertTrue(markdown.contains("| 名称 | 数量 |\n"), markdown);
        assertTrue(markdown.contains("| 库存 | 产地 |\n"), markdown);
        // 不允许出现跨 sheet 拼接的表头行
        assertFalse(markdown.contains("数量 | 库存"), markdown);
        // 各 sheet 行数据与各自表头列对齐
        assertTrue(markdown.contains("| 值A| 值B |\n"), markdown);
        assertTrue(markdown.contains("| 值C| 值D |\n"), markdown);
    }

    @Test
    void shouldSeparateSheetsWithBlankLine() {
        byte[] bytes = writeTwoSheetWorkbook(
                List.of(List.of("名称")), List.of(List.of("值A")),
                List.of(List.of("库存")), List.of(List.of("值C")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        // 两个表格之间用空行分隔，保证 Markdown 渲染为两张独立表格
        assertTrue(markdown.contains("| 值A |\n\n| 库存 |\n"), markdown);
    }
}
