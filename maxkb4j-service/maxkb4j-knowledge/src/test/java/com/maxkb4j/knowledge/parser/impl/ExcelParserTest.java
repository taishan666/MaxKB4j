package com.maxkb4j.knowledge.parser.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private byte[] writeWorkbookWithBlankFirstRow(List<List<String>> dataRows) throws IOException {
        // 首行为空行：EasyExcel 默认把首行当表头读入，空首行使 invokeHeadMap 得到空映射，模拟无表头 sheet
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            sheet.createRow(0);
            int r = 1;
            for (List<String> row : dataRows) {
                Row xssfRow = sheet.createRow(r++);
                for (int c = 0; c < row.size(); c++) {
                    xssfRow.createCell(c).setCellValue(row.get(c));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void shouldKeepDataColumnsBeyondHeaderWidth() {
        // 修复前：invoke 只按表头列数取值，数据行多出的列被静默丢弃；
        // 且更宽的行出现在后时，先加入的短行不补齐会与表头错位
        byte[] bytes = writeWorkbook(
                List.of(List.of("名称"), List.of("数量")),
                List.of(List.of("值A", "值B"), List.of("值C", "值D", "值E", "值F")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        // 表头按数据实际列数用占位名扩展
        assertTrue(markdown.contains("| 名称 | 数量 | 列3 | 列4 |\n"), markdown);
        // 更宽的行出现前加入的短行补齐空串，列数与表头对齐
        assertTrue(markdown.contains("| 值A| 值B| |  |"), markdown);
        assertTrue(markdown.contains("| 值C| 值D| 值E| 值F |"), markdown);
    }

    @Test
    void shouldSynthesizeHeadersWhenSheetHasNoHeader() throws IOException {
        // 修复前：无表头 sheet 的 headers 为空，数据行全部产出空列表且 appendSheet 直接跳过，整表数据静默丢失
        byte[] bytes = writeWorkbookWithBlankFirstRow(
                List.of(List.of("值A", "值B"), List.of("值C", "值D")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        assertTrue(markdown.contains("| 列1 | 列2 |\n"), markdown);
        assertTrue(markdown.contains("| 值A| 值B |"), markdown);
        assertTrue(markdown.contains("| 值C| 值D |"), markdown);
    }

    @Test
    void shouldEscapePipeInCell() {
        // 修复前：单元格中的竖线未转义直接拼入 Markdown 表格，破坏列结构（DocParser 已转义）
        byte[] bytes = writeWorkbook(
                List.of(List.of("名称"), List.of("备注")),
                List.of(List.of("值|A", "a|b|c")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        assertTrue(markdown.contains("值\\|A"), markdown);
        assertTrue(markdown.contains("a\\|b\\|c"), markdown);
        assertFalse(markdown.contains("值|A"), markdown);
    }

    @Test
    void shouldNormalizeCarriageReturnInCell() {
        // 单元格内含 Windows 换行时 \r 会被移除、\n 转为 <br>，避免破坏表格行
        byte[] bytes = writeWorkbook(
                List.of(List.of("名称")),
                List.of(List.of("值\r\nA")));
        String markdown = parser.handle(new ByteArrayInputStream(bytes));
        assertFalse(markdown.contains("\r"), markdown);
        assertTrue(markdown.contains("值<br>A"), markdown);
    }
}
