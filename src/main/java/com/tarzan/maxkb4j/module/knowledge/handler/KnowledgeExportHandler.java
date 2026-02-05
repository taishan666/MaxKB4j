package com.tarzan.maxkb4j.module.knowledge.handler;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.knowledge.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemParagraphMapper;
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
    public List<DatasetExcel> getDatasetExcelByDoc(DocumentEntity doc) {
        List<DatasetExcel> list = new ArrayList<>();
        LambdaQueryWrapper<ParagraphEntity> queryWrapper = Wrappers.<ParagraphEntity>lambdaQuery()
                .eq(ParagraphEntity::getDocumentId, doc.getId());
        List<ParagraphEntity> paragraphs = paragraphMapper.selectList(queryWrapper);
        for (ParagraphEntity paragraph : paragraphs) {
            DatasetExcel excel = new DatasetExcel();
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
        try (ExcelWriter excelWriter = EasyExcel.write(outputStream, DatasetExcel.class).build()) {
            for (DocumentEntity doc : docs) {
                List<DatasetExcel> list = getDatasetExcelByDoc(doc);
                if (list.isEmpty()) {
                    log.debug("文档 {} 没有数据，跳过该sheet", doc.getName());
                    continue; // 跳过空数据
                }
                WriteSheet writeSheet = EasyExcel.writerSheet(doc.getName()).build();
                excelWriter.write(list, writeSheet);
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
}