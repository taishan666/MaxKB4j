package com.maxkb4j.knowledge.parser.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.maxkb4j.knowledge.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExcelParser implements DocumentParser {

    @Override
    public List<String> getExtensions() {
        return List.of(".xls", ".xlsx");
    }

    @Override
    public String handle(InputStream inputStream) {
        // 每个 sheet 独立成表：doReadAll() 顺序解析所有 sheet，invokeHeadMap/doAfterAllAnalysed 每 sheet 各回调一次，
        // 因此缓冲区必须在每个 sheet 开始时重置、结束时输出，否则表头跨 sheet 累积会导致列错乱
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        StringBuilder markdown = new StringBuilder();
        EasyExcel.read(inputStream, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                headers.clear();
                rows.clear();
                // 按列索引顺序保存表头；空表头单元格（缺键或 null 值）用占位名补齐，避免漏列或 null 元素
                int lastCol = -1;
                for (Integer key : headMap.keySet()) {
                    lastCol = Math.max(lastCol, key);
                }
                for (int i = 0; i <= lastCol; i++) {
                    String header = headMap.get(i);
                    headers.add(header == null || header.isBlank() ? "列" + (i + 1) : header.trim());
                }
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // 以数据实际列数为准：行宽超出表头时用占位名扩展表头，避免多出的列被静默丢弃；
                // 无表头 sheet（headMap 为空）也由此按数据列数生成占位表头，避免整表数据丢失
                int lastCol = -1;
                for (Integer key : data.keySet()) {
                    lastCol = Math.max(lastCol, key);
                }
                for (int i = headers.size(); i <= lastCol; i++) {
                    headers.add("列" + (i + 1));
                }
                List<String> row = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    String value = data.get(i);
                    row.add(value == null ? "" : value.trim());
                }
                rows.add(row);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                appendSheet(markdown, headers, rows);
                headers.clear();
                rows.clear();
            }
        }).doReadAll();
        return markdown.toString();
    }

    /**
     * 将单个 sheet 的表头和数据行追加为一段 Markdown 表格，多个 sheet 之间用空行分隔
     */
    private void appendSheet(StringBuilder markdown, List<String> headers, List<List<String>> rows) {
        if (headers.isEmpty()) {
            return;
        }
        if (markdown.length() > 0) {
            markdown.append("\n");
        }
        // 表头
        markdown.append("| ").append(String.join(" | ", headers)).append(" |\n");
        // 分隔线
        markdown.append("|");
        markdown.append(" --- |".repeat(headers.size()));
        markdown.append("\n");
        // 数据行
        for (List<String> row : rows) {
            // 表头可能被后续更宽的数据行扩展，补齐先加入的短行，保证各行列数与表头一致
            while (row.size() < headers.size()) {
                row.add("");
            }
            for (String cell : row) {
                // 转义竖线并处理换行，避免单元格内容破坏 Markdown 表格（与 DocParser 口径一致）
                markdown.append("| ").append(cell.replace("|", "\\|").replace("\r", "").replace("\n", "<br>"));
            }
            markdown.append(" |\n");
        }
    }
}
