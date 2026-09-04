package com.maxkb4j.tool.handler;

import cn.hutool.json.JSONUtil;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.exception.ToolImportExportException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * 工具导入导出处理器
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolImportExportHandler {

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
}