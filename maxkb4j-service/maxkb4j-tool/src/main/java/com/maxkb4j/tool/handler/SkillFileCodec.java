package com.maxkb4j.tool.handler;

import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.exception.ToolImportExportException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.regex.Pattern;

/**
 * SKILL 工具文件编解码器
 *
 * <p>负责 SKILL 工具 code 字段与 OSS 文件内容之间的双向转换：
 * <ul>
 *   <li>导出（embed）：将 code（OSS 文件 ID）替换为文件字节的 Base64 编码，使导出文件自包含、可跨环境导入；</li>
 *   <li>导入（restore）：将 Base64 编码解码还原为文件字节并上传至 OSS，以返回的文件 ID 作为 code。</li>
 * </ul>
 *
 * @author tarzan
 */
@Component
@RequiredArgsConstructor
public class SkillFileCodec {

    /** OSS 文件 ID 格式（24 位十六进制）：旧版导出的 .mk 中 SKILL 工具 code 为该格式 */
    private static final Pattern OSS_FILE_ID_PATTERN = Pattern.compile("^[0-9a-f]{24}$");

    private static final String SKILL_FILE_SUFFIX = ".zip";
    private static final String SKILL_FILE_CONTENT_TYPE = "application/zip";

    private final IOssService ossService;

    /**
     * 判断 code 是否为旧版导出格式的 OSS 文件 ID（跨环境无法还原文件内容）
     *
     * @param code 工具 code 字段
     * @return true 表示为旧版 OSS 文件 ID 格式
     */
    public boolean isLegacyOssFileId(String code) {
        return code != null && OSS_FILE_ID_PATTERN.matcher(code).matches();
    }

    /**
     * 导出嵌入：以 OSS 文件 ID 查询文件字节数据，返回其 Base64 编码
     *
     * @param toolName 工具名称（用于异常提示）
     * @param fileId   OSS 文件 ID
     * @return 文件字节的 Base64 编码
     */
    public String embed(String toolName, String fileId) {
        if (StringUtils.isBlank(fileId)) {
            throw new ToolImportExportException("SKILL 工具未关联文件，无法导出: " + toolName);
        }
        byte[] fileBytes = ossService.getBytes(fileId);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new ToolImportExportException("SKILL 工具文件不存在或为空，无法导出: " + toolName);
        }
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    /**
     * 导入还原：将 Base64 编码解码为文件字节并上传至 OSS，返回新的文件 ID
     *
     * @param toolName       工具名称（用于异常提示与生成存储文件名）
     * @param base64Content  文件字节的 Base64 编码
     * @return 上传后的 OSS 文件 ID
     */
    public String restore(String toolName, String base64Content) {
        if (StringUtils.isBlank(base64Content)) {
            throw new ToolImportExportException("SKILL 工具文件内容为空，无法导入: " + toolName);
        }
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new ToolImportExportException("SKILL 工具文件内容不是合法的 Base64 编码: " + toolName, e);
        }
        if (fileBytes.length == 0) {
            throw new ToolImportExportException("SKILL 工具文件内容为空，无法导入: " + toolName);
        }
        return ossService.storeFile(fileBytes, toolName + SKILL_FILE_SUFFIX, SKILL_FILE_CONTENT_TYPE);
    }
}
