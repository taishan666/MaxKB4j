package com.maxkb4j.knowledge.handler;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.mapper.ProblemParagraphMapper;
import com.maxkb4j.knowledge.vo.ProblemParagraphVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 知识库导出处理器
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeExportHandler {

    private static final int PARAGRAPH_BATCH_SIZE = 500;

    private final ParagraphMapper paragraphMapper;
    private final ProblemParagraphMapper problemParagraphMapper;

    /**
     * 根据文档实体获取Excel数据列表
     *
     * @param doc 文档实体
     * @return Excel数据列表
     */
    public List<KnowledgeExcel> getDatasetExcelByDoc(DocumentEntity doc) {
        List<KnowledgeExcel> list = new ArrayList<>();
        processParagraphBatches(doc, list::addAll);
        return list;
    }

    private boolean processParagraphBatches(DocumentEntity doc, Consumer<List<KnowledgeExcel>> consumer) {
        String lastId = null;
        boolean hasData = false;
        while (true) {
            LambdaQueryWrapper<ParagraphEntity> queryWrapper = Wrappers.<ParagraphEntity>lambdaQuery()
                    .select(ParagraphEntity::getId, ParagraphEntity::getTitle, ParagraphEntity::getContent)
                    .eq(ParagraphEntity::getDocumentId, doc.getId())
                    .gt(lastId != null, ParagraphEntity::getId, lastId)
                    .orderByAsc(ParagraphEntity::getId)
                    .last("LIMIT " + PARAGRAPH_BATCH_SIZE);
            List<ParagraphEntity> paragraphs = paragraphMapper.selectList(queryWrapper);
            if (paragraphs.isEmpty()) {
                break;
            }
            hasData = true;
            consumer.accept(toExcelRows(paragraphs));
            if (paragraphs.size() < PARAGRAPH_BATCH_SIZE) {
                break;
            }
            lastId = paragraphs.get(paragraphs.size() - 1).getId();
        }
        return hasData;
    }

    private List<KnowledgeExcel> toExcelRows(List<ParagraphEntity> paragraphs) {
        List<String> paragraphIds = paragraphs.stream()
                .map(ParagraphEntity::getId)
                .toList();
        Map<String, String> problemsByParagraphId = problemParagraphMapper
                .getProblemContentsByParagraphIds(paragraphIds).stream()
                .filter(problem -> StringUtils.isNotBlank(problem.getContent()))
                .collect(Collectors.groupingBy(ProblemParagraphVO::getParagraphId,
                        Collectors.mapping(ProblemParagraphVO::getContent, Collectors.joining("\n"))));
        List<KnowledgeExcel> rows = new ArrayList<>(paragraphs.size());
        for (ParagraphEntity paragraph : paragraphs) {
            KnowledgeExcel excel = new KnowledgeExcel();
            excel.setTitle(paragraph.getTitle());
            excel.setContent(paragraph.getContent());
            excel.setProblems(problemsByParagraphId.getOrDefault(paragraph.getId(), ""));
            rows.add(excel);
        }
        return rows;
    }

    /**
     * 设置 Excel 响应头
     */
    public void setExcelResponseHeader(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20"); // 兼容部分浏览器空格变+的问题
        response.setHeader("Content-disposition", "attachment;filename=" + encodedName + ".xlsx");
    }

    /**
     * 设置 ZIP 响应头
     */
    public void setZipResponseHeader(HttpServletResponse response, String fileName) {
        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + encodedName + ".zip");
    }

    /**
     * 写入多Sheet的Excel文件到输出流
     */
    public void writeMultiSheetExcel(OutputStream outputStream, List<DocumentEntity> docs) {
        try (ExcelWriter excelWriter = EasyExcel.write(outputStream, KnowledgeExcel.class)
                .autoCloseStream(false).build()) {
            boolean hasData = false;
            for (DocumentEntity doc : docs) {
                WriteSheet writeSheet = EasyExcel.writerSheet(doc.getName()).build();
                boolean docHasData = processParagraphBatches(doc, rows -> excelWriter.write(rows, writeSheet));
                if (!docHasData) {
                    log.debug("文档 {} 没有数据，跳过该sheet", doc.getName());
                    continue;
                }
                hasData = true;
            }
            // 如果没有任何数据，写入一个空sheet以确保Excel文件有效
            if (!hasData) {
                WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();
                excelWriter.write(new ArrayList<>(), writeSheet);
            }
        }
    }

    /**
     * 将Excel内容打包为ZIP格式并写入响应
     */
    public void writeExcelToZipAndResponse(List<DocumentEntity> docs, String fileName, HttpServletResponse response) throws IOException {
        setZipResponseHeader(response, fileName);
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            zipOut.putNextEntry(new ZipEntry(fileName + ".xlsx"));
            writeMultiSheetExcel(zipOut, docs);
            zipOut.closeEntry();
            zipOut.finish();
            response.getOutputStream().flush();
        }
    }

    /**
     * 导出知识库ZIP包（包含knowledge.json和knowledge.xlsx）
     *
     * @param docs           文档列表
     * @param knowledgeName  知识库名称
     * @param knowledgeDesc  知识库描述
     * @param knowledgeType  知识库类型
     * @param meta           元数据
     * @param fileSizeLimit  文件大小限制
     * @param fileCountLimit 文件数量限制
     * @param response       HTTP响应
     * @throws IOException IO异常
     */
    public void exportKnowledgeZip(List<DocumentEntity> docs, String knowledgeName, String knowledgeDesc,
                                   Integer knowledgeType, JSONObject meta, Integer fileSizeLimit,
                                   Integer fileCountLimit, HttpServletResponse response) throws IOException {
        setZipResponseHeader(response, knowledgeName);

        // 构建 knowledge.json
        JSONObject knowledgeJson = new JSONObject();
        knowledgeJson.put("name", knowledgeName);
        knowledgeJson.put("desc", knowledgeDesc);
        knowledgeJson.put("type", knowledgeType);
        knowledgeJson.put("meta", meta != null ? meta : new JSONObject());
        knowledgeJson.put("fileSizeLimit", fileSizeLimit != null ? fileSizeLimit : 100);
        knowledgeJson.put("fileCountLimit", fileCountLimit != null ? fileCountLimit : 50);
        knowledgeJson.put("tags", new JSONArray());
        byte[] jsonBytes = knowledgeJson.toJSONString().getBytes(StandardCharsets.UTF_8);

        // 创建 ZIP 并写入响应
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // 添加 knowledge.json
            zipOut.putNextEntry(new ZipEntry("knowledge.json"));
            zipOut.write(jsonBytes);
            zipOut.closeEntry();

            // 添加 knowledge.xlsx
            zipOut.putNextEntry(new ZipEntry("knowledge.xlsx"));
            writeMultiSheetExcel(zipOut, docs);
            zipOut.closeEntry();
            zipOut.finish();
            response.getOutputStream().flush();
        }
    }
}