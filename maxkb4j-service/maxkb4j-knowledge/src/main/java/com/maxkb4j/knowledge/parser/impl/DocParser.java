package com.maxkb4j.knowledge.parser.impl;

import com.maxkb4j.knowledge.parser.DocumentParser;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.oss.service.IOssService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
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
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class DocParser implements DocumentParser {

    private final IOssService mongoFileService;

    @Override
    public List<String> getExtensions() {
        return List.of(".doc", ".docx");
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
        return convertXhtmlToMarkdown(xhtml, embeddedResources);
    }

    private String convertXhtmlToMarkdown(String xhtml, Map<String, byte[]> embeddedImages) {
        // 使用 jsoup 解析 XHTML
        Document doc = Jsoup.parse(xhtml, "", org.jsoup.parser.Parser.xmlParser());
        Element body = doc.body();
        // 使用 flexmark 或手动转换（这里用简易规则模拟）
        return simpleXhtmlToMarkdown(body, embeddedImages);
    }


    private String simpleXhtmlToMarkdown(Element element, Map<String, byte[]> embeddedImages) {
        StringBuilder md = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                String html = node.outerHtml();
                String text = StringEscapeUtils.unescapeHtml4(html);
                if (!text.isEmpty()&&!"\n".equals(text)) {
                    md.append(text);
                }
            } else if (node instanceof Element child) {
                String tagName = child.tagName().toLowerCase();
                switch (tagName) {
                    case "h1":
                        md.append(parseTitleToMarkdown("#", child));
                        break;
                    case "h2":
                        md.append(parseTitleToMarkdown("##", child));
                        break;
                    case "h3":
                        md.append(parseTitleToMarkdown("###", child));
                        break;
                    case "h4":
                        md.append(parseTitleToMarkdown("####", child));
                        break;
                    case "h5":
                        md.append(parseTitleToMarkdown("#####", child));
                        break;
                    case "h6":
                        md.append(parseTitleToMarkdown("######", child));
                        break;
                    case "p":
                        md.append(simpleXhtmlToMarkdown(child, embeddedImages)).append("\n");
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
                    case "strong", "b":
                        // md.append("<b>").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("</b>");
                        md.append(simpleXhtmlToMarkdown(child, embeddedImages));
                        break;
                    case "i":
                        //md.append("_").append(simpleXhtmlToMarkdown(child, embeddedImages)).append("_");
                        md.append(simpleXhtmlToMarkdown(child, embeddedImages));
                        break;
                    case "br":
                        md.append("  \n");
                        break;
                    case "img":
                        String src = child.attr("src");
                        if (src.startsWith("embedded:")) {
                            String resourceName = src.split(":")[1];
                            byte[] imageData = embeddedImages.get(resourceName);
                            if (imageData != null && imageData.length > 0) {
                                OssFile uploadedImage = mongoFileService.uploadFile(resourceName, imageData);
                                md.append("![](").append(uploadedImage.getUrl()).append(")");
                            } else {
                                md.append("![Embedded image not found](").append(src).append(")");
                            }
                        } else {
                            md.append("![](").append(src).append(")");
                        }
                        break;
                    case "a":
                        String href = child.attr("href");
                        String linkText = simpleXhtmlToMarkdown(child, embeddedImages);
                        if (href.isEmpty()) {
                            md.append(linkText);
                        } else {
                            md.append("[").append(linkText).append("](").append(href).append(")");
                        }
                        break;
                    case "table":
                        md.append("\n").append(parseTableToMarkdown(child, embeddedImages)).append("\n");
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


    private String parseTitleToMarkdown(String titleLevel, Element child) {
        String text = child.text();
        if (!text.isEmpty()) {
            return "\n" + titleLevel + " " + text + "\n";
        }
        return "";
    }

    private String parseTableToMarkdown(Element table, Map<String, byte[]> embeddedImages) {
        StringBuilder md = new StringBuilder();
        Elements rows = new Elements();
        // 直接子元素中找 tr
        for (Element e : table.children()) {
            if ("tr".equals(e.tagName())) {
                rows.add(e);
            } else if ("thead".equals(e.tagName()) ||
                    "tbody".equals(e.tagName()) ||
                    "tfoot".equals(e.tagName())) {
                // 在 thead/tbody/tfoot 下找直接子 tr
                for (Element tr : e.children()) {
                    if ("tr".equals(tr.tagName())) {
                        rows.add(tr);
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            return "";
        }
        List<String> headers = new ArrayList<>();
        List<List<String>> dataRows = new ArrayList<>();
        boolean isFirstRow = true;
        for (Element row : rows) {
            Elements cells = row.select("th, td");
            List<String> cellTexts = new ArrayList<>();
            for (Element cell : cells) {
                Elements tables = cell.getElementsByTag("table");
                if (tables.isEmpty()) {
                    // 递归处理单元格内的内容（可能包含 p, strong, a 等）
                    String cellContent = simpleXhtmlToMarkdown(cell, embeddedImages).trim();
                    // 转义管道符和换行，避免破坏 Markdown 表格
                    cellContent = cellContent.replace("|", "\\|").replace("\n", " ");
                    cellTexts.add(cellContent);
                }
            }
            if (cellTexts.isEmpty()) {
                continue; // 跳过空行
            }
            if (isFirstRow && !row.select("th").isEmpty()) {
                // 如果第一行包含 <th>，视为表头
                headers.addAll(cellTexts);
            } else {
                dataRows.add(cellTexts);
                if (isFirstRow) {
                    // 即使没有 <th>，也把第一行当表头（常见于 Word/PDF 转出的 XHTML）
                    headers.addAll(cellTexts);
                    dataRows.clear(); // 清除刚加的“假表头”行
                }
            }
            isFirstRow = false;
        }
        if (headers.isEmpty() && dataRows.isEmpty()) {
            return "";
        }
        // 确保所有行列数一致（取最大列数）
        int maxCols = headers.size();
        for (List<String> row : dataRows) {
            maxCols = Math.max(maxCols, row.size());
        }
        while (headers.size() < maxCols) {
            headers.add("");
        }
        // 写表头
        md.append("| ").append(String.join(" | ", headers)).append(" |\n");
        // 写分隔线
        List<String> aligners = new ArrayList<>();
        for (int i = 0; i < maxCols; i++) {
            aligners.add("---");
        }
        md.append("| ").append(String.join(" | ", aligners)).append(" |\n");
        // 写数据行
        for (List<String> row : dataRows) {
            while (row.size() < maxCols) {
                row.add("");
            }
            md.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        return md.toString();
    }

}