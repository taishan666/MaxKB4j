package com.tarzan.maxkb4j.core.workflow.parser.impl;

import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.parser.DocumentParser;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.ToXMLContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class PdfParser implements DocumentParser {

    private final MongoFileService fileService;

    @Override
    public boolean support(String fileName) {
        return fileName.endsWith(".pdf") || fileName.endsWith(".doc") || fileName.endsWith(".docx");
    }

    @Override
    public String handle(InputStream inputStream) {
        Parser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();
        // 配置 Office 和 PDF
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        officeConfig.setIncludeHeadersAndFooters(false);
        parseContext.set(OfficeParserConfig.class, officeConfig);

        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
        parseContext.set(PDFParserConfig.class, pdfConfig);

        // 🌟 关键：用于存储提取出的嵌入资源（如图片）
        Map<String, byte[]> embeddedResources = new ConcurrentHashMap<>();

        // 自定义 EmbeddedDocumentExtractor
        parseContext.set(
                EmbeddedDocumentExtractor.class,
                new EmbeddedDocumentExtractor() {
                    @Override
                    public boolean shouldParseEmbedded(Metadata metadata) {
                        // 只处理图片类型
                        String contentType = metadata.get(Metadata.CONTENT_TYPE);
                        return contentType != null && contentType.startsWith("image/");
                    }

                    @Override
                    public void parseEmbedded(
                            InputStream inputStream,
                            ContentHandler handler,
                            Metadata metadata,
                            boolean outputHtml)
                            throws IOException {
                        String resourceName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY);
                        // 读取图片字节
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        inputStream.transferTo(baos);
                        byte[] imageBytes = baos.toByteArray();

                        // 存储到 map，供后续替换 src 使用
                        embeddedResources.put(resourceName, imageBytes);
                    }
                }
        );
        // ✅ 正确创建 ToXMLContentHandler
        ToXMLContentHandler handler = new ToXMLContentHandler();
        // 解析文档为 XHTML
        try {
            parser.parse(inputStream, handler, metadata, parseContext);
        } catch (IOException | SAXException | TikaException e) {
            throw new RuntimeException(e);
        }
        String xhtml = handler.toString();
        return convertXhtmlToMarkdown(xhtml,embeddedResources);
    }

    private String convertXhtmlToMarkdown(String xhtml,Map<String, byte[]> embeddedImages) {
        // 使用 jsoup 解析 XHTML
        Document doc = Jsoup.parse(xhtml, "", org.jsoup.parser.Parser.xmlParser());
        Element body = doc.body();
        // 使用 flexmark 或手动转换（这里用简易规则模拟）
        // 实际项目建议引入 flexmark-java + jsoup-to-markdown 转换器
        return simpleXhtmlToMarkdown(body,embeddedImages);
    }


    private String simpleXhtmlToMarkdown(Element element, Map<String, byte[]> embeddedImages) {
        StringBuilder md = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                String text = ((TextNode) node).text().trim();
                if (!text.isEmpty()) {
                    md.append(text);
                }
            } else if (node instanceof Element child) {
                String tagName = child.tagName().toLowerCase();
                switch (tagName) {
                    case "h1":
                        md.append("\n# ").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n");
                        break;
                    case "h2":
                        md.append("\n## ").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n");
                        break;
                    case "h3":
                        md.append("\n### ").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n");
                        break;
                    case "h4":
                        md.append("\n#### ").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n");
                        break;
                    case "h5":
                        md.append("\n##### ").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n");
                        break;
                    case "h6":
                        md.append("\n###### ").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n");
                        break;
                    case "p":
                        md.append("\n").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n");
                        break;
                    case "ul":
                        for (Element li : child.select("li")) {
                            md.append("\n- ").append(simpleXhtmlToMarkdown(li, embeddedImages));
                        }
                        md.append("\n");
                        break;
                    case "ol":
                        int i = 1;
                        for (Element li : child.select("li")) {
                            md.append("\n").append(i++).append(". ").append(simpleXhtmlToMarkdown(li, embeddedImages));
                        }
                        md.append("\n");
                        break;
                    case "pre":
                        md.append("\n```\n").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n```\n");
                        break;
                    case "code":
                        md.append("`").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("`");
                        break;
                    case "strong","b":
                        md.append("<strong>").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("</strong>");
                        break;
                    case "em":
                    case "i":
                        md.append("_").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("_");
                        break;
                    case "br":
                        md.append("  \n");
                        break;
                    case "img":
                        String src = child.attr("src");
                        if (src.startsWith("embedded:")) {
                            String resourceName =src.split(":")[1];
                            byte[] imageData = embeddedImages.get(resourceName);
                            if (imageData != null && imageData.length > 0) {
                                SysFile uploadedImage = fileService.uploadFile(resourceName, imageData);
                                md.append("![](").append(uploadedImage.getUrl()).append(")");
                            } else {
                                md.append("![Embedded image not found](").append(src).append(")");
                            }
                        } else {
                            md.append("![](").append(src).append(")");
                        }
                        break;
                    case "a":
                        // 可选：处理超链接
                        String href = child.attr("href");
                        String linkText = simpleXhtmlToMarkdown(child, embeddedImages);
                        if (href.isEmpty()) {
                            md.append(linkText);
                        } else {
                            md.append("[").append(linkText).append("](").append(href).append(")");
                        }
                        break;
                    default:
                        // 对未知标签，递归处理其内容（不加额外格式）
                        md.append(simpleXhtmlToMarkdown(child, embeddedImages));
                        break;
                }
            }
        }
        return md.toString();
    }

}