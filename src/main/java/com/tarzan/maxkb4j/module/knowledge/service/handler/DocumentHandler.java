package com.tarzan.maxkb4j.module.knowledge.service.handler;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.fastjson.JSON;
import com.tarzan.maxkb4j.listener.DataListener;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentSimple;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphSimple;
import com.tarzan.maxkb4j.module.knowledge.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.txt.UniversalEncodingDetector;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 文档处理器
 * 处理文档上传、解析、导入等操作
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentHandler {

    private final MongoFileService mongoFileService;

    /**
     * 处理ZIP格式的QA文件
     */
    public List<DocumentSimple> processZipQaFile(MultipartFile zipFile) throws IOException {
        List<DocumentSimple> docs =new ArrayList<>();
        try (InputStream fis = zipFile.getInputStream(); ZipArchiveInputStream zipIn = new ZipArchiveInputStream(fis)) {
            ArchiveEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (!entry.isDirectory() && isExcelOrCsv(entry.getName())) {
                    byte[] content = zipIn.readAllBytes();
                    docs.addAll(processQaFile(content, entry.getName()));
                    break;
                }
            }
        }
        return docs;
    }

    /**
     * 判断文件名是否为Excel或CSV格式
     */
    public boolean isExcelOrCsv(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv");
    }

    /**
     * 处理QA文件（Excel或CSV）
     *
     * @return 解析后的文档列表
     */
    public List<DocumentSimple> processQaFile(byte[] bytes, String fileName) {
        List<DocumentSimple> docs = new ArrayList<>();
        // 判断是否为 CSV 文件（不区分大小写）
        boolean isCsv = fileName.toLowerCase().endsWith(".csv");
        String fileId = mongoFileService.storeFile(bytes, fileName, null);
        if (isCsv) {
            DocumentSimple docSimple = new DocumentSimple();
            docSimple.setName(fileName);
            docSimple.setSourceFileId(fileId);
            List<ParagraphSimple> paragraphs = new ArrayList<>();
            // === 处理 CSV 文件 ===
            TikaInputStream tikaStream = TikaInputStream.get(bytes);
            EncodingDetector detector = new UniversalEncodingDetector();
            Charset charset;
            try {
                charset = detector.detect(tikaStream, new Metadata());
                String charsetName = charset != null ? charset.name() : "UTF-8";
                log.info("检测到 CSV 文件编码: {}", charsetName);
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
                    for (CSVRecord record : csvParser) {
                        ParagraphSimple paragraph = ParagraphSimple.builder()
                                .title(record.get(0))
                                .content(record.get(1))
                                .build();
                        if (StringUtils.isNotBlank(record.get(2))) {
                            String[] problems = record.get(2).split("\n");
                            paragraph.setProblemList(Arrays.asList(problems));
                        }
                        paragraphs.add(paragraph);
                    }
                    docSimple.setParagraphs(paragraphs);
                    docs.add(docSimple);
                }
            } catch (IOException e) {
                log.warn("无法检测 CSV 编码，使用 UTF-8 默认", e);
            }
        } else {
            // === 原有 Excel 逻辑保持不变 ===
            DataListener<DatasetExcel> dataListener = new DataListener<>();
            try (ExcelReader excelReader = EasyExcel.read(new ByteArrayInputStream(bytes), DatasetExcel.class, dataListener).build()) {
                List<ReadSheet> sheets = excelReader.excelExecutor().sheetList();
                for (ReadSheet sheet : sheets) {
                    DocumentSimple docSimple = new DocumentSimple();
                    String sheetName = StringUtils.defaultIfBlank(sheet.getSheetName(), fileName);
                    docSimple.setName(sheetName);
                    docSimple.setSourceFileId(fileId);
                    List<ParagraphSimple> paragraphs = new ArrayList<>();
                    log.info("正在读取 Sheet: {}", sheet.getSheetName());
                    excelReader.read(sheet);
                    List<DatasetExcel> dataList = dataListener.getDataList();
                    for (DatasetExcel data : dataList) {
                        log.info("在Sheet {} 中读取到一条数据: {}", sheet.getSheetName(), JSON.toJSONString(data));
                        ParagraphSimple paragraph = ParagraphSimple.builder()
                                .title(data.getTitle())
                                .content(data.getContent())
                                .build();
                        if (StringUtils.isNotBlank(data.getProblems())) {
                            String[] problems = data.getProblems().split("\n");
                            paragraph.setProblemList(Arrays.asList(problems));
                        }
                        paragraphs.add(paragraph);
                    }
                    docSimple.setParagraphs(paragraphs);
                    docs.add(docSimple);
                    dataListener.clear();
                }
            } catch (Exception e) {
                log.error("读取 Excel 失败: {}", e.getMessage(), e);
                throw new RuntimeException("读取 Excel 失败", e);
            }
        }
        // 返回解析的文档列表
        return docs;
    }

    /**
     * 处理QA文件（Excel或CSV）
     *
     * @return 解析后的文档列表
     */
    public List<DocumentSimple> processTable(byte[] bytes, String fileName) {
        List<DocumentSimple> docs = new ArrayList<>();
        // 判断是否为 CSV 文件（不区分大小写）
        boolean isCsv = fileName.toLowerCase().endsWith(".csv");
        String fileId = mongoFileService.storeFile(bytes, fileName, null);
        DocumentSimple docSimple = new DocumentSimple();
        docSimple.setName(fileName);
        docSimple.setSourceFileId(fileId);
        List<ParagraphSimple> paragraphs = new ArrayList<>();
        if (isCsv) {
            // === 处理 CSV 文件 ===
            TikaInputStream tikaStream = TikaInputStream.get(bytes);
            EncodingDetector detector = new UniversalEncodingDetector();
            Charset charset;
            try {
                charset = detector.detect(tikaStream, new Metadata());
                String charsetName = charset != null ? charset.name() : "UTF-8";
                log.info("检测到 CSV 文件编码: {}", charsetName);
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
                    List<String> headerNames =csvParser.getHeaderNames();
                    for (CSVRecord record : csvParser) {
                        List<String> row = new ArrayList<>();
                        for (String headerName : headerNames) {
                            row.add(headerName+":"+record.get(headerName));
                        }
                        ParagraphSimple paragraph = ParagraphSimple.builder()
                                .title("")
                                .content(String.join("|", row))
                                .build();
                        paragraphs.add(paragraph);
                    }
                    docSimple.setParagraphs(paragraphs);
                    docs.add(docSimple);
                }
            } catch (IOException e) {
                log.warn("无法检测 CSV 编码，使用 UTF-8 默认", e);
            }
        } else {
            // === 原有 Excel 逻辑保持不变 ===
            EasyExcel.read(new ByteArrayInputStream(bytes), new AnalysisEventListener<Map<Integer, String>>() {
                Map<Integer, String> headMap = new LinkedHashMap<>();
                // 表头信息会在此方法中获取
                @Override
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                    this.headMap = headMap;
                }
                // 每一行数据都会调用此方法
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    // String sheetName = context.readSheetHolder().getSheetName();
                    List<String> row = new ArrayList<>();
                    for (Integer i : data.keySet()) {
                        String value = data.get(i) == null ? "" : data.get(i);
                        row.add(headMap.get(i)+":"+value);
                    }
                    ParagraphSimple paragraph = ParagraphSimple.builder()
                            .title("")
                            .content(String.join("|", row))
                            .build();
                    paragraphs.add(paragraph);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    log.info("所有数据解析完成！");
                }
            }).doReadAll();
            docSimple.setParagraphs(paragraphs);
            docs.add(docSimple);
        }
        // 返回解析的文档列表
        return docs;
    }

}