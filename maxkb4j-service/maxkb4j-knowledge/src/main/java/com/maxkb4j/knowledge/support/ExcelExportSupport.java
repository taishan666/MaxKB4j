package com.maxkb4j.knowledge.support;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Excel/ZIP 导出辅助工具：统一响应头、sheet 名规范化、流式 ZIP 打包。
 */
public final class ExcelExportSupport {

    private static final int SHEET_NAME_MAX_LEN = 31;
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String ZIP_CONTENT_TYPE = "application/zip";

    private ExcelExportSupport() {
    }

    public static void prepareExcelResponse(HttpServletResponse response, String fileName) {
        prepareResponse(response, fileName, EXCEL_CONTENT_TYPE, ".xlsx");
    }

    public static void prepareZipResponse(HttpServletResponse response, String fileName) {
        prepareResponse(response, fileName, ZIP_CONTENT_TYPE, ".zip");
    }

    private static void prepareResponse(HttpServletResponse response, String fileName, String contentType, String suffix) {
        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + encoded + suffix);
    }

    /**
     * Excel 单 sheet 名长度上限 31，超长截断；包含扩展名时去除扩展名后再截断。
     */
    public static String normalizeSheetName(String rawName) {
        if (rawName == null) return "Sheet1";
        int dotIdx = rawName.lastIndexOf('.');
        String base = dotIdx > 0 ? rawName.substring(0, dotIdx) : rawName;
        return base.length() > SHEET_NAME_MAX_LEN ? base.substring(0, SHEET_NAME_MAX_LEN) : base;
    }

    /**
     * 通用多 sheet 写入：把元素列表按 sheet 写入同一个 ExcelWriter。
     */
    public static <T, E> void writeSheets(ExcelWriter writer,
                                          List<T> items,
                                          Function<T, String> sheetNameFn,
                                          Function<T, List<E>> dataFn) {
        for (T item : items) {
            WriteSheet sheet = EasyExcel.writerSheet(normalizeSheetName(sheetNameFn.apply(item))).build();
            writer.write(dataFn.apply(item), sheet);
        }
    }

    /**
     * 流式把多 sheet Excel 直接写入 ZIP，再写入响应流，避免内存中持有两份缓冲。
     */
    public static <T, E> void writeZippedExcel(OutputStream output,
                                               String entryName,
                                               Class<E> excelClass,
                                               List<T> items,
                                               Function<T, String> sheetNameFn,
                                               Function<T, List<E>> dataFn) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(output)) {
            zipOut.putNextEntry(new ZipEntry(entryName + ".xlsx"));
            // EasyExcel 关闭时会 close 底层流，包一层防止 zipOut 被提前关闭
            try (ExcelWriter writer = EasyExcel.write(new NonClosingOutputStream(zipOut), excelClass).build()) {
                writeSheets(writer, items, sheetNameFn, dataFn);
            }
            zipOut.closeEntry();
        }
    }

    private static final class NonClosingOutputStream extends FilterOutputStream {
        NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
