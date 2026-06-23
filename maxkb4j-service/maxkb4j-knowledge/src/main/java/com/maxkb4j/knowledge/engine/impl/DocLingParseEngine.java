package com.maxkb4j.knowledge.engine.impl;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.options.*;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.maxkb4j.knowledge.engine.DocumentParseEngine;
import com.maxkb4j.knowledge.engine.props.DocLingProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.docling.DoclingDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DocLing 文档解析引擎（自托管模式）
 * <p>
 * 调用自建 DocLing 服务（同步返回）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocLingParseEngine implements DocumentParseEngine {

    public static final String NAME = "doc-ling";


    private final DocLingProperties properties;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean support(String fileName) {
        if (!properties.isEnabled() || fileName == null) {
            return false;
        }
        if (StrUtil.isBlank(properties.getBaseUrl())) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return properties.getSupportedExtensions().stream().anyMatch(lower::endsWith);
    }

    @Override
    public String extractText(String fileName, InputStream inputStream) {
        byte[] bytes = IoUtil.readBytes(inputStream);
        String url = buildUrl(properties.getBaseUrl());
        DoclingServeApi api = DoclingServeApi.builder()
                .baseUrl(url)
                .apiKey(properties.getApiKey())
                .connectTimeout(Duration.of(properties.getTimeout(), TimeUnit.SECONDS.toChronoUnit()))
                .build();
        ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                .fromFormats(List.of(InputFormat.PDF, InputFormat.DOCX, InputFormat.CSV, InputFormat.PPTX,InputFormat.XLSX, InputFormat.HTML))
                .toFormat(OutputFormat.MARKDOWN)
                .pdfBackend(PdfBackend.PYPDFIUM2)
                .pipeline(ProcessingPipeline.STANDARD)
                .doCodeEnrichment(properties.isEnableCode())
                .doFormulaEnrichment(properties.isEnableFormula())
                .includeImages(true)
                .imageExportMode(ImageRefMode.EMBEDDED)
                .doTableStructure(properties.isEnableTable())
                .tableMode(TableFormerMode.ACCURATE)
                .tableCellMatching(true)
                .doOcr(properties.isEnableOcr())
                .ocrEngine(OcrEngine.AUTO)
                .build();
        DoclingDocumentParser parser = new DoclingDocumentParser(api, options);
        Document document = parser.parse(new ByteArrayInputStream(bytes));
        return document.text();
    }


    private String buildUrl(String base) {
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

}
