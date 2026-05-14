package com.maxkb4j.knowledge.handler;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.mapper.ProblemParagraphMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
        LambdaQueryWrapper<ParagraphEntity> queryWrapper = Wrappers.<ParagraphEntity>lambdaQuery()
                .eq(ParagraphEntity::getDocumentId, doc.getId());
        List<ParagraphEntity> paragraphs = paragraphMapper.selectList(queryWrapper);
        for (ParagraphEntity paragraph : paragraphs) {
            KnowledgeExcel excel = new KnowledgeExcel();
            excel.setTitle(paragraph.getTitle());
            excel.setContent(paragraph.getContent());
            List<ProblemEntity> problemEntities = problemParagraphMapper.getProblemsByParagraphId(paragraph.getId());
            StringBuilder sb = new StringBuilder();
            if (!CollectionUtils.isEmpty(problemEntities)) {
                List<String> problems = problemEntities.stream()
                        .map(ProblemEntity::getContent)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
                String result = String.join("\n", problems);
                sb.append(result);
            }
            excel.setProblems(sb.toString());
            list.add(excel);
        }
        return list;
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
    public void writeMultiSheetExcel(java.io.OutputStream outputStream, List<DocumentEntity> docs) {
        try (ExcelWriter excelWriter = EasyExcel.write(outputStream, KnowledgeExcel.class).build()) {
            boolean hasData = false;
            for (DocumentEntity doc : docs) {
                List<KnowledgeExcel> list = getDatasetExcelByDoc(doc);
                if (list.isEmpty()) {
                    log.debug("文档 {} 没有数据，跳过该sheet", doc.getName());
                    continue; // 跳过空数据
                }
                hasData = true;
                WriteSheet writeSheet = EasyExcel.writerSheet(doc.getName()).build();
                excelWriter.write(list, writeSheet);
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
        try (ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(zipBuffer)) {

            // 生成 Excel 内容到内存
            try (ByteArrayOutputStream excelBuffer = new ByteArrayOutputStream()) {
                writeMultiSheetExcel(excelBuffer, docs);
                // 添加到 ZIP
                String entryName = fileName + ".xlsx";
                zipOut.putNextEntry(new ZipEntry(entryName));
                zipOut.write(excelBuffer.toByteArray());
                zipOut.closeEntry();
            }
            // 写回 HTTP 响应
            zipBuffer.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    /**
     * 导出知识库ZIP包（包含knowledge.json和knowledge.xlsx）
     * @param docs 文档列表
     * @param knowledgeName 知识库名称
     * @param knowledgeDesc 知识库描述
     * @param knowledgeType 知识库类型
     * @param meta 元数据
     * @param fileSizeLimit 文件大小限制
     * @param fileCountLimit 文件数量限制
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    public void exportKnowledgeZip(List<DocumentEntity> docs, String knowledgeName, String knowledgeDesc,
                                   Integer knowledgeType, JSONObject meta, Integer fileSizeLimit,
                                   Integer fileCountLimit, HttpServletResponse response) throws IOException {
        setZipResponseHeader(response, knowledgeName);
        
        // 先在内存中生成 Excel
        ByteArrayOutputStream excelBuffer = new ByteArrayOutputStream();
        writeMultiSheetExcel(excelBuffer, docs);
        byte[] excelBytes = excelBuffer.toByteArray();
        excelBuffer.close();
        
        // 构建 knowledge.json
        JSONObject knowledgeJson = new JSONObject();
        knowledgeJson.put("name", knowledgeName);
        knowledgeJson.put("desc", knowledgeDesc);
        knowledgeJson.put("type", knowledgeType);
        knowledgeJson.put("meta", meta != null ? meta : new JSONObject());
        knowledgeJson.put("file_size_limit", fileSizeLimit != null ? fileSizeLimit : 100);
        knowledgeJson.put("file_count_limit", fileCountLimit != null ? fileCountLimit : 50);
        knowledgeJson.put("tags", new com.alibaba.fastjson.JSONArray());
        byte[] jsonBytes = knowledgeJson.toJSONString().getBytes(StandardCharsets.UTF_8);
        
        // 创建 ZIP 并写入响应
        ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
        try {
            // 添加 knowledge.json
            zipOut.putNextEntry(new ZipEntry("knowledge.json"));
            zipOut.write(jsonBytes);
            zipOut.closeEntry();
            
            // 添加 knowledge.xlsx
            zipOut.putNextEntry(new ZipEntry("knowledge.xlsx"));
            zipOut.write(excelBytes);
            zipOut.closeEntry();
        } finally {
            zipOut.finish();
            zipOut.flush();
            response.getOutputStream().flush();
        }
    }
}