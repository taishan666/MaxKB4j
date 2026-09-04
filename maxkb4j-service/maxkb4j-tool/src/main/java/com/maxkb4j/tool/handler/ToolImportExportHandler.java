package com.maxkb4j.tool.handler;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.tool.dto.ToolExportData;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.exception.ToolImportExportException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 工具导入导出处理器
 *
 * <p>仅负责导入/导出流程编排：
 * <ul>
 *   <li>导出：构建自包含的导出产物（{@link #prepareExport}），再写入 HTTP 响应；</li>
 *   <li>导入：解析上传文件并重置属性（{@link #importTool}）；</li>
 *   <li>SKILL 文件内容的 Base64 编解码委托给 {@link SkillFileCodec}。</li>
 * </ul>
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolImportExportHandler {

    private final UserContext userContext;
    private final SkillFileCodec skillFileCodec;

    /**
     * 导出工具到 HTTP 响应
     *
     * @param entity   工具实体
     * @param response HTTP 响应
     */
    public void exportTool(ToolEntity entity, HttpServletResponse response) {
        ToolExportData exportData = prepareExport(entity);
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String fileName = URLEncoder.encode(exportData.fileName(), StandardCharsets.UTF_8);
            response.setHeader("Content-disposition", "attachment;filename=" + fileName);
            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(exportData.content());
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("Failed to export tool: {}", entity.getName(), e);
            throw new ToolImportExportException("Failed to export tool: " + e.getMessage(), e);
        }
    }

    /**
     * 构建导出产物：SKILL 工具先嵌入文件内容，再序列化为 JSON 字节
     *
     * <p>不依赖 Servlet API，可独立测试。
     *
     * @param entity 工具实体
     * @return 导出产物（文件名 + 文件内容）
     */
    public ToolExportData prepareExport(ToolEntity entity) {
        if (entity == null) {
            throw new ToolImportExportException("Tool does not exist, cannot export");
        }
        if (ToolConstants.ToolType.SKILL.equals(entity.getToolType())) {
            entity.setCode(skillFileCodec.embed(entity.getName(), entity.getCode()));
        }
        byte[] content = JSON.toJSONString(entity).getBytes(StandardCharsets.UTF_8);
        return new ToolExportData(entity.getName() + ToolConstants.FileType.TOOL_EXTENSION, content);
    }

    /**
     * 导入工具：解析文件、还原 SKILL 文件并重置属性
     *
     * @param file     上传的工具文件（.mk）
     * @param folderId 目标文件夹 ID
     * @return 解析后的工具实体（未持久化）
     */
    public ToolEntity importTool(MultipartFile file, String folderId) {
        ToolEntity tool = parseToolFile(file);
        if (ToolConstants.ToolType.SKILL.equals(tool.getToolType())) {
            tool.setCode(skillFileCodec.restore(tool.getName(), tool.getCode()));
        }
        resetImportedAttributes(tool, folderId);
        return tool;
    }

    /**
     * 批量嵌入 SKILL 工具文件内容（导出用）
     *
     * <p>将 SKILL 工具的 code（OSS 文件 ID）替换为文件字节的 Base64 编码，使导出文件自包含。
     *
     * @param toolList 工具列表
     */
    public void embedSkillFileContents(List<ToolDTO> toolList) {
        if (CollectionUtils.isEmpty(toolList)) {
            return;
        }
        for (ToolDTO tool : toolList) {
            if (ToolConstants.ToolType.SKILL.equals(tool.getToolType())) {
                tool.setCode(skillFileCodec.embed(tool.getName(), tool.getCode()));
            }
        }
    }

    /**
     * 批量还原 SKILL 工具文件（导入用）
     *
     * <p>将 SKILL 工具的 code（Base64 编码的文件内容）解码并上传至 OSS，
     * 以返回的文件 ID 作为 code。旧版导出的 OSS 文件 ID 格式无法还原，保留原值并记录警告。
     *
     * @param toolList 工具列表
     */
    public void restoreSkillFiles(List<ToolDTO> toolList) {
        if (CollectionUtils.isEmpty(toolList)) {
            return;
        }
        for (ToolDTO tool : toolList) {
            if (!ToolConstants.ToolType.SKILL.equals(tool.getToolType())) {
                continue;
            }
            if (skillFileCodec.isLegacyOssFileId(tool.getCode())) {
                log.warn("SKILL tool [{}] code is an OSS file ID (legacy export format), cannot restore file content", tool.getName());
                continue;
            }
            tool.setCode(skillFileCodec.restore(tool.getName(), tool.getCode()));
        }
    }

    /**
     * 解析上传的工具文件为实体
     */
    private ToolEntity parseToolFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ToolImportExportException("The uploaded file cannot be empty");
        }
        try {
            String text = IoUtil.readToString(file.getInputStream());
            ToolEntity tool = JSON.parseObject(text, ToolEntity.class);
            if (tool == null) {
                throw new ToolImportExportException("The tool file format is invalid");
            }
            return tool;
        } catch (ToolImportExportException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to import tool", e);
            throw new ToolImportExportException("Failed to import tool: " + e.getMessage(), e);
        }
    }

    /**
     * 重置导入工具的属性：清空 ID、默认未启用、设置目标文件夹与当前用户
     */
    private void resetImportedAttributes(ToolEntity tool, String folderId) {
        tool.setId(null);
        tool.setIsActive(false);
        tool.setFolderId(folderId);
        tool.setUserId(userContext.getUserId());
    }
}
