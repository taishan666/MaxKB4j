package com.maxkb4j.tool.handler;

import cn.hutool.json.JSONUtil;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.exception.ToolImportExportException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 工具导入导出处理器
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolImportExportHandler {

    /** OSS 文件 ID 格式（24 位十六进制）：旧版导出的 .mk 中 SKILL 工具 code 为该格式 */
    private static final Pattern OSS_FILE_ID_PATTERN = Pattern.compile("^[0-9a-f]{24}$");

    private final UserContext userContext;
    private final IOssService ossService;

    /**
     * 导出工具到文件
     *
     * <p>SKILL 类型工具的 code 字段存储的是 OSS 文件 ID，导出时需将其替换为
     * 文件内容的 Base64 编码，使导出文件自包含、可跨环境导入。
     *
     * @param entity 工具实体
     * @param response HTTP响应
     */
    public void exportTool(ToolEntity entity, HttpServletResponse response) {
        if (entity == null) {
            throw new ToolImportExportException("工具不存在，无法导出");
        }
        try {
            if (ToolConstants.ToolType.SKILL.equals(entity.getToolType())) {
                embedSkillFileContent(entity);
            }
            byte[] bytes = Objects.requireNonNull(JSONUtil.toJsonStr(entity)).getBytes(StandardCharsets.UTF_8);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String fileName = URLEncoder.encode(entity.getName()+ ToolConstants.FileType.TOOL_EXTENSION, StandardCharsets.UTF_8);
            response.setHeader("Content-disposition", "attachment;filename=" + fileName);
            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(bytes);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("导出工具失败: {}", entity.getName(), e);
            throw new ToolImportExportException("导出工具失败: " + e.getMessage(), e);
        }
    }

    /**
     * SKILL 工具导出前置处理：以 code（OSS 文件 ID）查询文件字节数据，
     * 将字节数据 Base64 编码后回写到 code 字段，使导出文件自包含。
     */
    private void embedSkillFileContent(ToolEntity entity) {
        String fileId = entity.getCode();
        if (StringUtils.isBlank(fileId)) {
            throw new ToolImportExportException("SKILL 工具未关联文件，无法导出: " + entity.getName());
        }
        byte[] fileBytes = ossService.getBytes(fileId);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new ToolImportExportException("SKILL 工具文件不存在或为空，无法导出: " + entity.getName());
        }
        entity.setCode(Base64.getEncoder().encodeToString(fileBytes));
    }

    /**
     * 从文件导入工具
     *
     * @param file 上传的文件
     * @param folderId 文件夹ID
     * @return 导入的工具实体
     */
    public ToolEntity importTool(MultipartFile file, String folderId) {
        if (file == null || file.isEmpty()) {
            throw new ToolImportExportException("上传文件不能为空");
        }
        try {
            String text = IoUtil.readToString(file.getInputStream());
            ToolEntity tool = com.alibaba.fastjson.JSONObject.parseObject(text, ToolEntity.class);

            if (tool == null) {
                throw new ToolImportExportException("工具文件格式不正确");
            }
            if (ToolConstants.ToolType.SKILL.equals(tool.getToolType())) {
                restoreSkillFile(tool);
            }
            // 设置导入后的新属性
            tool.setId(null); // 清除ID，生成新ID
            tool.setIsActive(false); // 导入后默认非激活
            tool.setFolderId(folderId);
            tool.setUserId(userContext.getUserId()); // 设置当前用户
            return tool;
        } catch (Exception e) {
            log.error("导入工具失败", e);
            throw new ToolImportExportException("导入工具失败: " + e.getMessage(), e);
        }
    }

    /**
     * SKILL 工具导入后置处理：导出时 code 被替换为文件内容的 Base64 编码，
     * 导入时需将其解码还原为文件字节并上传至 OSS，以返回的文件 ID 作为 code。
     */
    private void restoreSkillFile(ToolEntity tool) {
        String base64Content = tool.getCode();
        if (StringUtils.isBlank(base64Content)) {
            throw new ToolImportExportException("SKILL 工具文件内容为空，无法导入: " + tool.getName());
        }
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new ToolImportExportException("SKILL 工具文件内容不是合法的 Base64 编码: " + tool.getName(), e);
        }
        if (fileBytes.length == 0) {
            throw new ToolImportExportException("SKILL 工具文件内容为空，无法导入: " + tool.getName());
        }
        String fileName = tool.getName() + ".zip";
        String fileId = ossService.storeFile(fileBytes, fileName, "application/zip");
        tool.setCode(fileId);
    }

    /**
     * 应用导出前置处理：将列表中 SKILL 工具的 code（OSS 文件 ID）替换为
     * 文件字节的 Base64 编码，使导出的应用文件自包含。
     *
     * @param toolList 工具 DTO 列表
     */
    public void embedSkillFileContents(List<ToolDTO> toolList) {
        if (CollectionUtils.isEmpty(toolList)) {
            return;
        }
        for (ToolDTO tool : toolList) {
            if (!ToolConstants.ToolType.SKILL.equals(tool.getToolType())) {
                continue;
            }
            String fileId = tool.getCode();
            if (StringUtils.isBlank(fileId)) {
                throw new ToolImportExportException("SKILL 工具未关联文件，无法导出: " + tool.getName());
            }
            byte[] fileBytes = ossService.getBytes(fileId);
            if (fileBytes == null || fileBytes.length == 0) {
                throw new ToolImportExportException("SKILL 工具文件不存在或为空，无法导出: " + tool.getName());
            }
            tool.setCode(Base64.getEncoder().encodeToString(fileBytes));
        }
    }

    /**
     * 应用导入后置处理：将列表中 SKILL 工具的 code（文件字节的 Base64 编码）
     * 解码还原为文件字节并上传至 OSS，以返回的文件 ID 作为 code。
     *
     * <p>旧版导出的 .mk 中 code 为 OSS 文件 ID，跨环境无法还原，保持原样。
     *
     * @param toolList 工具 DTO 列表
     */
    public void restoreSkillFiles(List<ToolDTO> toolList) {
        if (CollectionUtils.isEmpty(toolList)) {
            return;
        }
        for (ToolDTO tool : toolList) {
            if (!ToolConstants.ToolType.SKILL.equals(tool.getToolType())) {
                continue;
            }
            String code = tool.getCode();
            if (StringUtils.isBlank(code)) {
                throw new ToolImportExportException("SKILL 工具文件内容为空，无法导入: " + tool.getName());
            }
            if (OSS_FILE_ID_PATTERN.matcher(code).matches()) {
                log.warn("SKILL 工具 [{}] 的 code 为 OSS 文件 ID（旧版导出格式），无法还原文件内容", tool.getName());
                continue;
            }
            byte[] fileBytes;
            try {
                fileBytes = Base64.getDecoder().decode(code);
            } catch (IllegalArgumentException e) {
                throw new ToolImportExportException("SKILL 工具文件内容不是合法的 Base64 编码: " + tool.getName(), e);
            }
            if (fileBytes.length == 0) {
                throw new ToolImportExportException("SKILL 工具文件内容为空，无法导入: " + tool.getName());
            }
            String fileName = tool.getName() + ".zip";
            String fileId = ossService.storeFile(fileBytes, fileName, "application/zip");
            tool.setCode(fileId);
        }
    }
}