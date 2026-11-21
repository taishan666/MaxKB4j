package com.maxkb4j.knowledge.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ExcelExportSupport} sheet 名规范化与去重测试：
 * 非法字符替换、31 字符截断、重名追加序号，并以 POI 真实建表校验回环。
 */
class ExcelExportSupportTest {

    private static final int SHEET_NAME_MAX_LEN = 31;

    @Test
    void normalizeSheetName_truncatesToExcelLimit() {
        String name = ExcelExportSupport.normalizeSheetName("a".repeat(40));

        assertEquals("a".repeat(31), name);
    }

    @Test
    void normalizeSheetName_replacesIllegalCharsAndStripsExtension() {
        String name = ExcelExportSupport.normalizeSheetName("a/b\\c?d*e[f]g:h.xlsx");

        assertEquals("a b c d e f g h", name);
    }

    @Test
    void normalizeSheetName_stripsExtension() {
        assertEquals("report", ExcelExportSupport.normalizeSheetName("report.pdf"));
    }

    @Test
    void normalizeSheetName_blankOrIllegalOnlyFallsBackToDefault() {
        assertEquals("Sheet1", ExcelExportSupport.normalizeSheetName(null));
        assertEquals("Sheet1", ExcelExportSupport.normalizeSheetName(""));
        assertEquals("Sheet1", ExcelExportSupport.normalizeSheetName("???"));
    }

    @Test
    void normalizeSheetName_trimsEdgeApostrophesAndSpaces() {
        assertEquals("my doc", ExcelExportSupport.normalizeSheetName(" 'my doc ' "));

        // 截断到 31 字符后以单引号结尾，POI 会拒绝，需去掉
        assertEquals("a".repeat(30), ExcelExportSupport.normalizeSheetName("a".repeat(30) + "'"));
    }

    @Test
    void uniqueSheetNames_dedupesDuplicates() {
        List<String> names = ExcelExportSupport.uniqueSheetNames(List.of("报告", "报告", "报告"));

        assertEquals(List.of("报告", "报告(2)", "报告(3)"), names);
    }

    @Test
    void uniqueSheetNames_treatsCaseAsCollision() {
        // Excel 视大小写不同的同名 sheet 为冲突，去重需不区分大小写
        List<String> names = ExcelExportSupport.uniqueSheetNames(List.of("Report", "report"));

        assertEquals(List.of("Report", "report(2)"), names);
    }

    @Test
    void uniqueSheetNames_suffixKeepsNameWithinLimit() {
        String longName = "d".repeat(40);
        List<String> names = ExcelExportSupport.uniqueSheetNames(List.of(longName, longName));

        assertEquals("d".repeat(31), names.get(0));
        assertEquals("d".repeat(28) + "(2)", names.get(1));
        assertTrue(names.stream().allMatch(n -> n.length() <= SHEET_NAME_MAX_LEN));
    }

    @Test
    void uniqueSheetNames_blankEntriesGetDefaultThenDedup() {
        List<String> names = ExcelExportSupport.uniqueSheetNames(Arrays.asList(null, "", "???", "Sheet1"));

        assertEquals(List.of("Sheet1", "Sheet1(2)", "Sheet1(3)", "Sheet1(4)"), names);
    }

    @Test
    void uniqueSheetNames_suffixSkipsPreoccupiedCandidate() {
        // "(2)" 已被占位，第二个 "ab" 应跳到 "(3)"
        List<String> names = ExcelExportSupport.uniqueSheetNames(List.of("ab(2)", "ab", "ab"));

        assertEquals(List.of("ab(2)", "ab", "ab(3)"), names);
    }

    @Test
    void generatedNamesAcceptedByPoiWorkbook() throws Exception {
        List<String> names = ExcelExportSupport.uniqueSheetNames(Arrays.asList(
                "d".repeat(40), "d".repeat(40),
                "a/b\\c?d*e[f]g:h.xlsx",
                null, "???", "ends with '", "'starts",
                "History"));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            for (String name : names) {
                workbook.createSheet(name);
            }
            assertEquals(names.size(), workbook.getNumberOfSheets());
            for (int i = 0; i < names.size(); i++) {
                assertEquals(names.get(i), workbook.getSheetName(i));
            }
        }
    }

    @Test
    void writeSheetsProducesWorkbookWithNormalizedUniqueSheetNames() throws Exception {
        List<String> rawNames = List.of("a/b\\c?d*e[f]g:h.xlsx", "报告", "报告", "d".repeat(40));
        List<String> expected = ExcelExportSupport.uniqueSheetNames(rawNames);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ExcelWriter writer = EasyExcel.write(bos, KnowledgeExcel.class).build()) {
            ExcelExportSupport.writeSheets(writer, rawNames, name -> name, name -> new ArrayList<KnowledgeExcel>());
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bos.toByteArray()))) {
            assertEquals(expected.size(), workbook.getNumberOfSheets());
            for (int i = 0; i < expected.size(); i++) {
                assertEquals(expected.get(i), workbook.getSheetName(i));
            }
        }
    }
}
