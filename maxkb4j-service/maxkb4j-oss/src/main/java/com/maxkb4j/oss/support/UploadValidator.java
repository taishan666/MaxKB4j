package com.maxkb4j.oss.support;

import com.maxkb4j.common.exception.FileLimitExceededException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * 通用文件上传校验器：扩展名白名单 + 文件名安全检查。
 * <p>全局大小上限由 spring.servlet.multipart 配置约束，本校验器负责类型与文件名安全。</p>
 *
 * @author tarzan
 */
@Component
public class UploadValidator {

    /** 允许上传的扩展名白名单：图片 / 常见文档 / 音视频。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 图片
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "html", "csv",
            // 音频
            "mp3", "wav", "m4a", "ogg","acc", "flac",
            // 视频
            "mp4", "webm"
    );

    /**
     * 校验上传文件：非空、文件名安全、扩展名在白名单内。
     *
     * @throws FileLimitExceededException 校验失败（全局异常处理器映射为 400）
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileLimitExceededException("common.file.empty");
        }
        String filename = file.getOriginalFilename();
        if (StringUtils.isBlank(filename)) {
            throw new FileLimitExceededException("common.file.empty");
        }
        // 文件名安全检查：禁止路径分隔符与相对路径片段
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new FileLimitExceededException("common.file.type.not.allowed");
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new FileLimitExceededException("common.file.type.not.allowed");
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileLimitExceededException("common.file.type.not.allowed");
        }
    }
}
