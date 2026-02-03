package com.tarzan.maxkb4j.module.tool.handler;

import cn.hutool.json.JSONUtil;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.module.tool.consts.ToolConstants;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.exception.ToolImportExportException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 工具导入导出处理器
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolImportExportHandler {

    /**
     * 导出工具到文件
     *
     * @param entity 工具实体
     * @param response HTTP响应
     */
    public void exportTool(ToolEntity entity, HttpServletResponse response) {
        if (entity == null) {
            throw new ToolImportExportException("工具不存在，无法导出");
        }
        try {
            byte[] bytes = JSONUtil.toJsonStr(entity).getBytes(StandardCharsets.UTF_8);
            response.setContentType(ToolConstants.FileType.JSON_CONTENT_TYPE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String fileName = URLEncoder.encode(entity.getName(), StandardCharsets.UTF_8);
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ToolConstants.FileType.TOOL_EXTENSION);
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
            // 设置导入后的新属性
            tool.setId(null); // 清除ID，生成新ID
            tool.setIsActive(false); // 导入后默认非激活
            tool.setFolderId(folderId);
            tool.setUserId(StpKit.ADMIN.getLoginIdAsString()); // 设置当前用户
            return tool;
        } catch (Exception e) {
            log.error("导入工具失败", e);
            throw new ToolImportExportException("导入工具失败: " + e.getMessage(), e);
        }
    }
}