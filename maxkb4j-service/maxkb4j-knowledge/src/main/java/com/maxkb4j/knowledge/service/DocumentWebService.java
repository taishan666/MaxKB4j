package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.dto.ParagraphSimple;
import com.maxkb4j.knowledge.util.JsoupUtil;
import com.maxkb4j.knowledge.util.WebContentCleaner;
import org.jetbrains.annotations.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentWebService implements IDocumentWebService{

    private final DocumentSplitService documentSpiltService;
    private final DocumentParseService documentParseService;

    /** MD文件输出目录 */
    private static final String MD_OUTPUT_DIR = "logs/web_md";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public List<DocumentSimple> getDocumentList(String sourceUrl, String selector, boolean isRecursive) {
        List<DocumentSimple> documentList = new ArrayList<>();
        selector = StringUtils.isBlank(selector) ? "body" : selector;

        // 抓取主页面并去广告清洗
        Document mainDoc = JsoupUtil.getDocument(sourceUrl);
        WebContentCleaner.clean(mainDoc);
        processDocument(mainDoc, sourceUrl, selector, documentList);

        if (isRecursive) {
            Set<String> subLinks = extractSubLinks(mainDoc, sourceUrl);
            for (String subLink : subLinks) {
                Document subDoc = JsoupUtil.getDocument(subLink);
                WebContentCleaner.clean(subDoc);
                processDocument(subDoc, subLink, selector, documentList);
            }
        }
        return documentList;
    }

    public List<DocumentSimple> getWebDocuments(String sourceUrl, String selector, boolean isRecursive) {
        List<DocumentSimple> docs = getDocumentList(sourceUrl, selector, isRecursive);
        for (DocumentSimple doc : docs) {
            String content = doc.getContent();
            List<ParagraphSimple> paragraphs = documentSpiltService.smartSplit(content);
            doc.setParagraphs(paragraphs);
        }
        return docs;
    }


    private void processDocument(Document doc, String url, String selector, List<DocumentSimple> list) {
        Elements elements = doc.select(selector);
        String htmlContent = elements.html();
        if (htmlContent.isBlank()) {
            return;
        }

        // HTML→Markdown 转换
        String mdText = documentParseService.extractText("file.html",
                new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8)));

        // 保存 MD 文件到磁盘
        String mdFilePath = saveMdFile(url, doc.title(), mdText);

        JSONObject meta = new JSONObject();
        meta.put("sourceUrl", url);
        meta.put("selector", selector);
        meta.put("mdFilePath", mdFilePath);
        String title = doc.title().isBlank() ? url : doc.title();
        list.add(new DocumentSimple(title, mdText, meta));
    }

    /**
     * 将 Markdown 内容保存为 .md 文件
     */
    private String saveMdFile(String url, String title, String mdText) {
        try {
            Path dir = Paths.get(MD_OUTPUT_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String fileName = buildMdFileName(url, title);
            Path filePath = dir.resolve(fileName);
            Files.writeString(filePath, mdText, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return filePath.toAbsolutePath().toString();
        } catch (Exception e) {
            log.error("保存MD文件失败: url={}", url, e);
            return "save_failed: " + e.getMessage();
        }
    }

    /**
     * 根据 URL 和标题生成安全的 MD 文件名
     * 格式: yyyyMMdd_HHmmss_标题_路径摘要.md
     */
    private String buildMdFileName(String url, String title) {
        String timestamp = LocalDateTime.now().format(DT_FMT);

        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "").trim();
        if (safeTitle.length() > 30) {
            safeTitle = safeTitle.substring(0, 30);
        }
        if (safeTitle.isBlank()) {
            safeTitle = "untitled";
        }

        String pathSummary;
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isEmpty() || path.equals("/")) {
                pathSummary = "root";
            } else {
                pathSummary = path.replaceAll("[\\\\/:*?\"<>|]", "_")
                        .replaceAll("_+", "_")
                        .replaceAll("^_|_$", "");
                if (pathSummary.length() > 40) {
                    pathSummary = pathSummary.substring(0, 40);
                }
            }
        } catch (URISyntaxException e) {
            pathSummary = url.replaceAll("[\\\\/:*?\"<>|]", "_").substring(0, Math.min(url.length(), 40));
        }

        return timestamp + "_" + safeTitle + "_" + pathSummary + ".md";
    }


    private Set<String> extractSubLinks(Document doc, String sourceUrl) {
        Set<String> links = new LinkedHashSet<>();
        try {
            URI baseUri = new URI(sourceUrl);
            List<String> baseSegments = getStrings(baseUri);
            int targetDepth = baseSegments.size() + 1;

            String origin = baseUri.getScheme() + "://" + baseUri.getHost();
            int port = baseUri.getPort();
            if (port > 0 && ((baseUri.getScheme().equals("http") && port != 80) ||
                    (baseUri.getScheme().equals("https") && port != 443))) {
                origin += ":" + port;
            }

            Elements anchorTags = doc.select("a[href]");
            for (Element link : anchorTags) {
                String href = link.attr("href").trim();
                if (!href.startsWith("/")) {
                    continue;
                }
                if (href.contains("?") || href.contains("#")) {
                    continue;
                }
                String cleanHref = href.endsWith("/") ? href.substring(0, href.length() - 1) : href;
                if (cleanHref.isEmpty() || cleanHref.equals("/")) {
                    continue;
                }
                List<String> hrefSegments = new ArrayList<>();
                for (String seg : cleanHref.split("/")) {
                    if (!seg.isEmpty()) {
                        hrefSegments.add(seg);
                    }
                }
                if (hrefSegments.size() != targetDepth) {
                    continue;
                }
                if (hrefSegments.size() < baseSegments.size()) {
                    continue;
                }
                boolean prefixMatch = true;
                for (int i = 0; i < baseSegments.size(); i++) {
                    if (!hrefSegments.get(i).equals(baseSegments.get(i))) {
                        prefixMatch = false;
                        break;
                    }
                }
                if (prefixMatch) {
                    links.add(origin + href);
                }
            }

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid sourceUrl: " + sourceUrl, e);
        }

        return links;
    }

    private @NotNull List<String> getStrings(URI baseUri) {
        String basePath = baseUri.getPath();
        if (basePath == null || basePath.isEmpty() || basePath.equals("/")) {
            basePath = "";
        } else if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        List<String> baseSegments = new ArrayList<>();
        if (!basePath.isEmpty()) {
            for (String seg : basePath.split("/")) {
                if (!seg.isEmpty()) {
                    baseSegments.add(seg);
                }
            }
        }
        return baseSegments;
    }

}
