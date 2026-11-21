package com.maxkb4j.knowledge.parser.impl;

import com.maxkb4j.knowledge.parser.DocumentParser;
import com.maxkb4j.knowledge.util.EncodingDetectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.tika.io.TikaInputStream;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CsvParser implements DocumentParser {

    @Override
    public List<String> getExtensions() {
        return List.of(".csv");
    }

    @Override
    public String handle(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }

        try (TikaInputStream tikaStream = TikaInputStream.get(inputStream)) {
            // 检测字符编码（检测失败回退 UTF-8）并重置流，再按检测到的编码读取 CSV
            Charset charset = EncodingDetectUtil.detectAndReset(tikaStream);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(tikaStream, charset))) {
                CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                        .setHeader().setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .get();
                CSVParser csvParser = csvFormat.parse(reader);
                List<String> headers = new ArrayList<>(csvParser.getHeaderNames());
                List<List<String>> rows = new ArrayList<>();
                for (CSVRecord record : csvParser) {
                    List<String> row = new ArrayList<>();
                    for (String header : headers) {
                        String value = record.get(header);
                        if (value == null) {
                            value = "";
                        } else {
                            value = value.replaceAll("\n", "<br>").replaceAll("\r", "");
                        }
                        row.add(value);
                    }
                    rows.add(row);
                }
                // 构建 Markdown 表格
                StringBuilder markdown = new StringBuilder();
                if (!headers.isEmpty()) {
                    markdown.append("| ").append(String.join(" | ", headers)).append(" |\n");
                    markdown.append("|").append(" --- |".repeat(headers.size())).append("\n");
                    for (List<String> row : rows) {
                        markdown.append("| ").append(String.join(" | ", row)).append(" |\n");
                    }
                }
                return markdown.toString();
            }
        } catch (Exception e) {
            log.error("解析 CSV 文件失败", e);
            throw new RuntimeException("CSV 解析异常: " + e.getMessage(), e);
        }
    }
}