package com.maxkb4j.oss.support;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OSS 文件 HTTP 响应处理工具类。
 * <p>
 * 将文件下载/预览的响应头设置、内联预览判断、流写入等与具体存储实现无关的
 * 通用逻辑集中处理，使各 {@code IOssService} 实现只需关注自身的文件读取逻辑，
 * 避免在多个实现中重复编写 HTTP 响应代码。
 */
public final class FileResponseHelper {

    /** 无法识别类型时的默认 Content-Type。 */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private FileResponseHelper() {
    }

    /**
     * 判断给定 Content-Type 是否支持浏览器内联预览。
     */
    public static boolean isPreviewAble(String contentType) {
        return contentType != null
                && (contentType.startsWith("image/") || "application/pdf".equals(contentType));
    }

    /**
     * 根据文件名、Content-Type 与大小配置响应头。
     * <p>
     * 可预览类型使用 {@code inline}，其余类型以附件方式下载，文件名按 UTF-8 编码。
     * <b>必须在获取输出流之前调用。</b>
     *
     * @param response    HTTP 响应
     * @param filename    文件名（用于下载时的 Content-Disposition）
     * @param contentType 文件类型，为 {@code null} 时使用默认类型
     * @param length      文件大小（字节）
     */
    public static void configureHeaders(HttpServletResponse response, String filename,
                                        String contentType, long length) {
        String ct = (contentType != null) ? contentType : DEFAULT_CONTENT_TYPE;
        response.setContentType(ct);
        if (isPreviewAble(ct)) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
        } else {
            String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedFileName);
        }
        response.setContentLengthLong(length);
    }

    /**
     * 将输入流写入响应输出流并刷新。调用方负责关闭输入流。
     */
    public static void writeStream(HttpServletResponse response, InputStream inputStream) throws IOException {
        try (OutputStream outputStream = response.getOutputStream()) {
            inputStream.transferTo(outputStream);
            outputStream.flush();
        }
    }
}
