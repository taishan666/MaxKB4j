package com.tarzan.maxkb4j.module.knowledge.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class TemplateService {


    public void tableTemplateExport(String type, HttpServletResponse response) throws Exception {
        exportTemplate(type, response, "templates/document/knowledge_table_template.csv", "templates/document/knowledge_table_template.xlsx", "table_csv_template.csv", "table_excel_template.xlsx");
    }

    public void qaTemplateExport(String type, HttpServletResponse response) throws Exception {
        exportTemplate(type, response, "templates/document/knowledge_qa_template.csv", "templates/document/knowledge_qa_template.xlsx", "qa_csv_template.csv", "qa_excel_template.xlsx");
    }


    public void exportTemplate(String type, HttpServletResponse response, String csvPath, String excelPath, String csvFileName, String excelFileName) throws Exception {
        // 设置字符编码
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = "";
        String contentType = "";
        InputStream inputStream = null;
        ClassLoader classLoader = getClass().getClassLoader();
        if ("csv".equals(type)) {
            contentType = "text/csv";
            fileName = URLEncoder.encode(csvFileName, StandardCharsets.UTF_8);
            inputStream = classLoader.getResourceAsStream(csvPath);
        } else if ("excel".equals(type)) {
            contentType = "application/vnd.ms-excel"; // 更准确的Excel MIME类型
            fileName = URLEncoder.encode(excelFileName, StandardCharsets.UTF_8);
            inputStream = classLoader.getResourceAsStream(excelPath);
        }

        if (inputStream != null) {
            try (OutputStream outputStream = response.getOutputStream()) {
                // 设置响应内容类型和头部信息
                response.setContentType(contentType);
                response.setHeader("Content-disposition", "attachment;filename=" + fileName);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } finally {
                // 确保输入流被关闭，即使发生异常
                inputStream.close();
            }
        } else {
            throw new Exception("无法找到指定类型的模板文件");
        }
    }


}
