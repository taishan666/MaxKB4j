package com.tarzan.maxkb4j.core.workflow.parser.impl;

import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.parser.DocumentParser;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class PdfParser implements DocumentParser {

    private final MongoFileService fileService;

    @Override
    public boolean support(SysFile sysFile) {
        return sysFile.getName().endsWith(".pdf")||sysFile.getName().endsWith(".doc")||sysFile.getName().endsWith(".docx");
    }

    @Override
    public String handle(SysFile sysFile) {
        try {
            InputStream inputStream= fileService.getStream(sysFile.getFileId());
            // 初始化解析器、元数据和上下文
            Parser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();
            // ✅ 忽略 Office 文档中的页眉和页脚
            OfficeParserConfig officeParserConfig = new OfficeParserConfig();
            officeParserConfig.setIncludeHeadersAndFooters(false);
            parseContext.set(OfficeParserConfig.class, officeParserConfig);
            // ✅ PDF 配置，禁用 OCR(OCR 依赖 Tesseract 原生引擎,需要额外依赖 tesseract 和 tess4j,性能开销大)
            PDFParserConfig pdfParserConfig = new PDFParserConfig();
            pdfParserConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
            parseContext.set(PDFParserConfig.class, pdfParserConfig);
            Map<String, String> imageMap = new LinkedHashMap<>();
            // 自定义ContentHandler用于插入占位符
            class MarkdownImageHandler extends ContentHandlerDecorator {
                private final StringBuilder markdown = new StringBuilder();
                private String localName = null;

                @Override
                public void characters(char[] ch, int start, int length) {
                    String text = new String(ch, start, length);
                    if (this.localName.equals("h1")) {
                        markdown.append("# ").append(text);
                    } else if (this.localName.equals("p")) {
                        markdown.append("\n").append(text);
                    } else {
                        markdown.append(text);
                    }
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attrs) {
                    this.localName = localName;
                    if ("img".equals(localName)) { // 捕获图片节点
                        String src = attrs.getValue("src");
                        if (src != null && src.startsWith("embedded:")) {
                            String imageName = src.split(":")[1];
                            SysFile image = fileService.uploadFile(imageName, new byte[0]);
                            imageMap.put(imageName, image.getFileId());
                            markdown.append("![](").append(image.getUrl()).append(")\n");
                        }
                    }
                }
                public String getMarkdown() {
                    return markdown.toString();
                }
            }
            MarkdownImageHandler contentHandler = new MarkdownImageHandler();
            EmbeddedDocumentExtractor extractor = new EmbeddedDocumentExtractor() {
                @Override
                public boolean shouldParseEmbedded(Metadata metadata) {
                    // 只处理图片类型
                    return metadata.get(Metadata.CONTENT_TYPE) != null &&
                            metadata.get(Metadata.CONTENT_TYPE).startsWith("image/");
                }
                @Override
                public void parseEmbedded(InputStream inputStream, ContentHandler embeddedHandler, Metadata metadata, boolean b) {
                    String fileName = metadata.get("resourceName");
                    String fileId = imageMap.get(fileName);
                    fileService.updateFile(fileId, inputStream);
                }
            };
            parseContext.set(EmbeddedDocumentExtractor.class, extractor);
            // 开始解析文档
            try {
                parser.parse(inputStream, contentHandler, metadata, parseContext);
            } catch (IOException | SAXException | TikaException e) {
                throw new RuntimeException(e);
            }
            return contentHandler.getMarkdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
