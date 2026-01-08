package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.JsoupUtil;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentSimple;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphSimple;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
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
public class DocumentWebService {

    private final DocumentSpiltService documentSpiltService;

    public List<DocumentSimple> getDocumentList(String sourceUrl, String selector,boolean isRecursive) {
        List<DocumentSimple> documentList = new ArrayList<>();
        selector = StringUtils.isBlank(selector) ? "body" : selector;
        // 抓取主页面
        Document mainDoc = JsoupUtil.getDocument(sourceUrl);
        processDocument(mainDoc, sourceUrl, selector, documentList);
        if (isRecursive){
            Set<String> subLinks = extractSubLinks(mainDoc, sourceUrl);
            for (String subLink : subLinks) {
                Document subDoc = JsoupUtil.getDocument(subLink);
                processDocument(subDoc, subLink, selector, documentList);
            }
        }
        return documentList;
    }

    public List<DocumentSimple> getWebDocuments(String sourceUrl, String selector,boolean isRecursive) {
        List<DocumentSimple> docs = getDocumentList(sourceUrl, selector,isRecursive);
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
        FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
        String mdText = converter.convert(htmlContent);
        JSONObject meta = new JSONObject();
        meta.put("sourceUrl", url);
        meta.put("selector", selector);
        String title = doc.title().isBlank() ? url : doc.title();
        list.add(new DocumentSimple(title, mdText, meta));
    }


    private Set<String> extractSubLinks(Document doc, String sourceUrl) {
        Set<String> links = new LinkedHashSet<>();
        try {
            URI baseUri = new URI(sourceUrl);
            List<String> baseSegments = getStrings(baseUri);
            int targetDepth = baseSegments.size() + 1; // 我们要提取的深度
            // 构造 origin（协议 + 主机 + 端口）
            String origin = baseUri.getScheme() + "://" + baseUri.getHost();
            int port = baseUri.getPort();
            if (port > 0 && ((baseUri.getScheme().equals("http") && port != 80) ||
                    (baseUri.getScheme().equals("https") && port != 443))) {
                origin += ":" + port;
            }
            Elements anchorTags = doc.select("a[href]");
            for (Element link : anchorTags) {
                String href = link.attr("href").trim();
                // 跳过非相对路径（只处理以 / 开头的）
                if (!href.startsWith("/")) {
                    continue;
                }
                // 跳过带查询参数或锚点的
                if (href.contains("?") || href.contains("#")) {
                    continue;
                }
                // 标准化 href 路径
                String cleanHref = href.endsWith("/") ? href.substring(0, href.length() - 1) : href;
                if (cleanHref.isEmpty() || cleanHref.equals("/")) {
                    continue;
                }
                // 分割路径段
                List<String> hrefSegments = new ArrayList<>();
                for (String seg : cleanHref.split("/")) {
                    if (!seg.isEmpty()) {
                        hrefSegments.add(seg);
                    }
                }
                // 必须比 source 多一层
                if (hrefSegments.size() != targetDepth) {
                    continue;
                }
                // 必须以前缀匹配
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
                    links.add(origin + href); // 保留原始 href（含结尾 / 与否）
                }
            }

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid sourceUrl: " + sourceUrl, e);
        }

        return links;
    }

    private @NotNull List<String> getStrings(URI baseUri) {
        String basePath = baseUri.getPath();
        // 标准化路径：确保以 / 开头，不以 / 结尾（除非是根）
        if (basePath == null || basePath.isEmpty() || basePath.equals("/")) {
            basePath = "";
        } else if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        // 提取 source 的路径段
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