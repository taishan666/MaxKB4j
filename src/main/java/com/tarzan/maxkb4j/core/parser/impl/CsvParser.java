package com.tarzan.maxkb4j.core.parser.impl;

import com.tarzan.maxkb4j.core.parser.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.txt.UniversalEncodingDetector;
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
    public boolean support(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }

    @Override
    public String handle(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }

        try {
            // 将输入流包装为 TikaInputStream（支持 mark/reset）
            TikaInputStream tikaStream = TikaInputStream.get(inputStream);
            // 检测字符编码
            Metadata metadata = new Metadata();
            // 可选：设置文件名帮助检测
            // metadata.set(Metadata.RESOURCE_NAME_KEY, "file.csv");
            EncodingDetector detector = new UniversalEncodingDetector();
            Charset charset = detector.detect(tikaStream, metadata);
            String charsetName = charset != null ? charset.name() : "UTF-8";
            // 重要：重置流，因为 detect() 可能已读取部分内容
            tikaStream.reset();
            // 使用检测到的编码读取 CSV
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(tikaStream, Charset.forName(charsetName)))) {
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