package com.tarzan.maxkb4j.core.parser.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.tarzan.maxkb4j.core.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        // 用于保存表头和所有行数据
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        EasyExcel.read(inputStream, new AnalysisEventListener<Map<Integer, String>>() {
            Map<Integer, String> headMap = new LinkedHashMap<>();
            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                this.headMap = headMap;
                // 按列索引顺序保存表头
                for (int i = 0; i < headMap.size(); i++) {
                    headers.add(headMap.get(i));
                }
            }
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                List<String> row = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    String value = data.get(i);
                    row.add(value == null ? "" : value.trim());
                }
                rows.add(row);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                log.info("所有数据解析完成！");
            }
        }).doReadAll();
        // 构建 Markdown 表格
        StringBuilder markdown = new StringBuilder();
        // 表头
        if (!headers.isEmpty()) {
            markdown.append("| ").append(String.join(" | ", headers)).append(" |\n");
            // 分隔线
            markdown.append("|");
            markdown.append(" --- |".repeat(headers.size()));
            markdown.append("\n");

            // 数据行
            for (List<String> row : rows) {
                for (String cell : row) {
                    cell=cell.replaceAll("\n", "<br>");
                    markdown.append("| ").append(cell);
                }
                markdown.append(" |\n");
            }
        }
        return markdown.toString();
    }
}
