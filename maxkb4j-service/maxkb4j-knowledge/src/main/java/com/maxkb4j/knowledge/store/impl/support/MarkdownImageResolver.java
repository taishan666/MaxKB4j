package com.maxkb4j.knowledge.store.impl.support;

import dev.langchain4j.data.message.ImageContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 Markdown 文本中解析图片并转换为 langchain4j 的 {@link ImageContent}。
 * <p>该组件把「图片提取 / 下载 / Base64 编码 / MIME 类型推断」这一与向量存储无关的职责
 * 从 {@code PgVectorEmbeddingStoreImpl} 中剥离出来，使 store 只专注于向量的写入与检索。</p>
 * <p>所有对外链图片的下载失败都会被吞掉并返回 {@code null}，即单张图片解析失败不影响整体流程。</p>
 */
@Slf4j
@Component
public class MarkdownImageResolver {

    /**
     * 从 Markdown 文本中提取所有图片 URL。
     */
    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    /**
     * 下载图片用于转 Base64 Data URI 时的连接/读取超时（毫秒）。
     */
    private static final int IMAGE_CONNECT_TIMEOUT_MS = 5000;
    private static final int IMAGE_READ_TIMEOUT_MS = 10000;
    private static final String DEFAULT_MIME_TYPE = "image/png";

    /**
     * 项目服务端口，用于将相对图片路径拼接为可访问的完整地址。
     */
    @Value("${server.port:8080}")
    private int serverPort = 8080;

    /**
     * 解析 Markdown 文本中的所有图片，逐个下载并转换为 Base64 形式的 {@link ImageContent}。
     *
     * @param markdownText 含图片语法的 Markdown 文本
     * @return 解析成功的图片内容列表；无图片或全部失败时返回空列表（不为 {@code null}）
     */
    public List<ImageContent> resolveImageContents(String markdownText) {
        List<ImageContent> contents = new ArrayList<>();
        for (String imageUrl : extractImageUrls(markdownText)) {
            ImageContent imageContent = toBase64ImageContent(imageUrl);
            if (imageContent != null) {
                contents.add(imageContent);
            }
        }
        return contents;
    }

    /**
     * 提取 Markdown 文本中的所有图片 URL（去除可能包裹的尖括号）。
     */
    private List<String> extractImageUrls(String markdownText) {
        List<String> urls = new ArrayList<>();
        if (StringUtils.isEmpty(markdownText)) {
            return urls;
        }
        Matcher matcher = MD_IMAGE_PATTERN.matcher(markdownText);
        while (matcher.find()) {
            String url = matcher.group(1).trim();
            if (url.startsWith("<") && url.endsWith(">")) {
                url = url.substring(1, url.length() - 1);
            }
            urls.add(url);
        }
        return urls;
    }

    /**
     * 下载图片并将其转换为 Base64 编码的 Data URI，封装进 {@link ImageContent}。
     * <p>{@link ImageContent#from(String, String)} 会以 {@code data:<mimeType>;base64,<data>}
     * 的形式保存，从而避免下游模型直接访问外链图片。</p>
     *
     * @param imageUrl 图片地址（http/https 外链，或已经是 data: 形式的 Data URI）
     * @return 转换后的 {@link ImageContent}；下载或解析失败时返回 {@code null}
     */
    private ImageContent toBase64ImageContent(String imageUrl) {
        if (StringUtils.isBlank(imageUrl)) {
            return null;
        }
        // 已经是 Base64 Data URI，直接解析复用
        if (imageUrl.startsWith("data:")) {
            return parseDataUri(imageUrl);
        }
        // 相对路径拼接为完整的本机服务地址
        String fullUrl = resolveFullUrl(imageUrl);
        try {
            URL url = URI.create(fullUrl).toURL();
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(IMAGE_CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(IMAGE_READ_TIMEOUT_MS);
            byte[] bytes;
            String contentType;
            try (InputStream in = connection.getInputStream()) {
                bytes = in.readAllBytes();
                contentType = connection.getContentType();
            }
            String mimeType = resolveMimeType(contentType, fullUrl);
            String base64Data = Base64.getEncoder().encodeToString(bytes);
            return ImageContent.from(base64Data, mimeType);
        } catch (Exception e) {
            log.warn("Failed to download image for base64 encoding: {}, cause: {}", fullUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 将图片地址解析为可访问的完整 URL。
     * <p>若已经是 {@code http://} / {@code https://} 绝对地址则原样返回；
     * 否则视为相对访问路径，使用 {@code 127.0.0.1} 加项目服务端口拼接。</p>
     */
    private String resolveFullUrl(String imageUrl) {
        String trimmed = imageUrl.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        String path = trimmed.startsWith("./") ? trimmed.substring(1) : trimmed;
        return "http://127.0.0.1:" + serverPort + path;
    }

    /**
     * 解析形如 {@code data:image/png;base64,xxxx} 的 Data URI 为 {@link ImageContent}。
     */
    private ImageContent parseDataUri(String dataUri) {
        try {
            int commaIdx = dataUri.indexOf(',');
            if (commaIdx < 0) {
                return null;
            }
            String meta = dataUri.substring(5, commaIdx); // 去掉 "data:" 前缀
            String base64Data = dataUri.substring(commaIdx + 1);
            String mimeType = DEFAULT_MIME_TYPE;
            int semicolonIdx = meta.indexOf(';');
            if (semicolonIdx > 0) {
                mimeType = meta.substring(0, semicolonIdx);
            } else if (!meta.isBlank()) {
                mimeType = meta;
            }
            return ImageContent.from(base64Data, mimeType);
        } catch (Exception e) {
            log.warn("Failed to parse data uri for image content, cause: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 优先使用响应头 Content-Type，缺失或为通用二进制流时回退到按 URL 扩展名猜测。
     */
    private String resolveMimeType(String contentType, String imageUrl) {
        if (StringUtils.isNotBlank(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType.trim())) {
            int semicolonIdx = contentType.indexOf(';');
            return semicolonIdx > 0 ? contentType.substring(0, semicolonIdx).trim() : contentType.trim();
        }
        return guessMimeTypeFromUrl(imageUrl);
    }

    /**
     * 根据 URL 路径扩展名猜测图片 MIME 类型，无法识别时默认 {@code image/png}。
     */
    private String guessMimeTypeFromUrl(String imageUrl) {
        String path = imageUrl;
        int queryIdx = path.indexOf('?');
        if (queryIdx > 0) {
            path = path.substring(0, queryIdx);
        }
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < path.length() - 1) {
            String ext = path.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
            switch (ext) {
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                case "webp":
                    return "image/webp";
                case "bmp":
                    return "image/bmp";
                case "svg":
                    return "image/svg+xml";
                default:
                    break;
            }
        }
        return DEFAULT_MIME_TYPE;
    }
}
