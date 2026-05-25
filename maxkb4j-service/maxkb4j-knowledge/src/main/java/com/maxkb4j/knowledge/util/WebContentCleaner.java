package com.maxkb4j.knowledge.util;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Web内容清洗工具
 * 负责去除网页中的广告、脚本、样式等噪音内容，提取干净的正文HTML
 *
 * @author maxkb4j
 */
public class WebContentCleaner {

    // ======================== 广告/噪音 标签 ========================
    private static final Set<String> NOISE_TAGS = new HashSet<>(Arrays.asList(
            "script", "style", "noscript", "iframe", "object", "embed",
            "nav", "footer", "aside", "form", "input", "button", "select",
            "textarea", "link", "meta", "svg", "canvas", "applet", "audio", "video"
    ));

    // ======================== 广告/噪音 class/id 关键词 ========================
    private static final Pattern NOISE_CLASS_PATTERN = Pattern.compile(
            "(?i)(ad|ads|advert|advertisement|banner|sponsor|popup|pop-up|modal|overlay|" +
            "sidebar|side-bar|widget|social|share|sharing|comment|recommend|related|" +
            "hot|trending|suggest|subscribe|newsletter|cookie|consent|gdpr|" +
            "toolbar|menu|search-box|searchbar|login|register|signup|signin|sign-up|sign-in|" +
            "pagination|pager|breadcrumb|footer|header|navbar|nav-bar|" +
            "adsbygoogle|adsense|doubleclick|taboola|outbrain)"
    );

    private static final Pattern NOISE_ID_PATTERN = Pattern.compile(
            "(?i)(ad|ads|advert|advertisement|banner|sponsor|popup|pop-up|modal|overlay|" +
            "sidebar|side-bar|widget|social|share|sharing|comment|recommend|related|" +
            "hot|trending|suggest|subscribe|newsletter|cookie|consent|gdpr|" +
            "toolbar|menu|search-box|searchbar|login|register|signup|signin|sign-up|sign-in|" +
            "pagination|pager|breadcrumb|footer|header|navbar|nav-bar)"
    );

    // ======================== 广告/噪音 属性选择器 ========================
    private static final List<String> NOISE_SELECTORS = Arrays.asList(
            "[aria-hidden=true]",
            "[hidden]",
            "[style*=\"display:none\"]",
            "[style*=\"display: none\"]",
            "[style*=\"visibility:hidden\"]",
            "[style*=\"visibility: hidden\"]",
            "[data-ad]",
            "[data-advertisement]",
            "[data-ads]",
            "[data-banner]",
            "[role=\"banner\"]",
            "[role=\"navigation\"]",
            "[role=\"complementary\"]",
            "[role=\"contentinfo\"]",
            "[role=\"search\"]",
            "[role=\"alert\"]",
            "[role=\"dialog\"]"
    );

    /**
     * 清洗Jsoup Document，去除广告和噪音内容
     *
     * @param doc Jsoup解析的HTML文档
     * @return 清洗后的Document（原地修改）
     */
    public static Document clean(Document doc) {
        removeNoiseTags(doc);
        removeNoiseByClassAndId(doc);
        removeNoiseBySelector(doc);
        removeComments(doc);
        removeEmptyElements(doc);
        return doc;
    }

    /**
     * 清洗Elements中的HTML内容
     */
    public static Elements cleanElements(Elements elements) {
        for (Element element : elements) {
            cleanElementRecursive(element);
        }
        return elements;
    }

    /**
     * 递归清洗单个Element及其子元素
     */
    private static void cleanElementRecursive(Element element) {
        Elements toRemove = new Elements();

        for (Element child : element.children()) {
            String tagName = child.tagName().toLowerCase();

            if (NOISE_TAGS.contains(tagName)) {
                toRemove.add(child);
                continue;
            }
            if (isNoiseClassOrId(child)) {
                toRemove.add(child);
                continue;
            }
            if (isHiddenElement(child)) {
                toRemove.add(child);
                continue;
            }

            cleanElementRecursive(child);
        }

        toRemove.remove();

        for (int i = element.childNodes().size() - 1; i >= 0; i--) {
            Node node = element.childNodes().get(i);
            if (node.nodeName().equals("#comment")) {
                node.remove();
            }
        }
    }

    /**
     * 移除噪音标签
     */
    private static void removeNoiseTags(Document doc) {
        for (String tag : NOISE_TAGS) {
            doc.select(tag).remove();
        }
    }

    /**
     * 根据class和id移除噪音元素
     */
    private static void removeNoiseByClassAndId(Document doc) {
        for (Element element : doc.getAllElements()) {
            String className = element.className();
            String id = element.id();

            boolean isNoise = false;
            if (!className.isEmpty() && NOISE_CLASS_PATTERN.matcher(className).find()) {
                isNoise = true;
            }
            if (!id.isEmpty() && NOISE_ID_PATTERN.matcher(id).find()) {
                isNoise = true;
            }

            if (isNoise && !element.tagName().equals("body") && !element.tagName().equals("html")) {
                element.remove();
            }
        }
    }

    /**
     * 根据CSS选择器移除噪音元素
     */
    private static void removeNoiseBySelector(Document doc) {
        for (String selector : NOISE_SELECTORS) {
            doc.select(selector).remove();
        }
    }

    /**
     * 移除HTML注释
     */
    private static void removeComments(Document doc) {
        for (Element element : doc.getAllElements()) {
            for (int i = element.childNodes().size() - 1; i >= 0; i--) {
                Node node = element.childNodes().get(i);
                if (node.nodeName().equals("#comment")) {
                    node.remove();
                }
            }
        }
    }

    /**
     * 移除空白/无用元素
     */
    private static void removeEmptyElements(Document doc) {
        boolean changed = true;
        while (changed) {
            changed = false;
            Elements allElements = doc.getAllElements();
            for (int i = allElements.size() - 1; i >= 0; i--) {
                Element element = allElements.get(i);
                String tag = element.tagName().toLowerCase();

                if (tag.equals("html") || tag.equals("head") || tag.equals("body") ||
                        tag.matches("h[1-6]") || tag.equals("p") || tag.equals("pre") ||
                        tag.equals("code") || tag.equals("blockquote") || tag.equals("li") ||
                        tag.equals("td") || tag.equals("th") || tag.equals("tr") ||
                        tag.equals("table") || tag.equals("ul") || tag.equals("ol") ||
                        tag.equals("dl") || tag.equals("dt") || tag.equals("dd") ||
                        tag.equals("img") || tag.equals("br") || tag.equals("hr")) {
                    continue;
                }

                String text = element.ownText().trim();
                int childCount = element.children().size();

                if (text.isEmpty() && childCount == 0) {
                    element.remove();
                    changed = true;
                }
            }
        }
    }

    private static boolean isNoiseClassOrId(Element element) {
        String className = element.className();
        String id = element.id();
        if (!className.isEmpty() && NOISE_CLASS_PATTERN.matcher(className).find()) {
            return true;
        }
        if (!id.isEmpty() && NOISE_ID_PATTERN.matcher(id).find()) {
            return true;
        }
        return false;
    }

    private static boolean isHiddenElement(Element element) {
        if (element.hasAttr("hidden") || element.hasAttr("aria-hidden")) {
            return true;
        }
        String style = element.attr("style");
        if (style.contains("display:none") || style.contains("display: none") ||
                style.contains("visibility:hidden") || style.contains("visibility: hidden")) {
            return true;
        }
        return false;
    }

}
