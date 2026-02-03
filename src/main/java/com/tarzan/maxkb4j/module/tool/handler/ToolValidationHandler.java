package com.tarzan.maxkb4j.module.tool.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarzan.maxkb4j.module.tool.consts.ToolConstants;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.exception.ToolValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * 工具验证处理器
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolValidationHandler {

    /**
     * 验证MCP服务器配置
     *
     * @param entity 工具实体
     * @return 是否有效
     */
    public boolean validateMcpServerConfig(ToolEntity entity) {
        if (ToolConstants.ToolType.MCP.equals(entity.getToolType())) {
            String jsonStr = entity.getCode();
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                return false;
            }
            JsonNode root;
            try {
                ObjectMapper mapper = new ObjectMapper();
                root = mapper.readTree(jsonStr);
            } catch (Exception e) {
                throw new ToolValidationException("MCP服务器配置解析失败: " + e.getMessage(), e);
            }
            // 1. 必须是对象
            if (!root.isObject()) {
                return false;
            }
            // 2. 遴历所有顶层字段
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode value = root.get(fieldName);

                // 每个值必须是对象
                if (!value.isObject()) {
                    return false;
                }

                // 检查是否存在 url 和 type 字段
                if (!value.has("url") || !value.has("type")) {
                    return false;
                }

                JsonNode urlNode = value.get("url");
                JsonNode typeNode = value.get("type");

                // url 必须是非空字符串
                if (!urlNode.isTextual() || urlNode.asText().trim().isEmpty()) {
                    return false;
                }

                // type 必须是 "streamable_http"
                boolean supported = typeNode.isTextual() && "streamable_http".equals(typeNode.asText());
                if (!supported) {
                    return false;
                }
            }
            return true; // 所有检查通过
        }
        return true;
    }

    /**
     * 验证工具实体的基本属性
     *
     * @param entity 工具实体
     * @return 验证结果
     */
    public boolean validateToolEntity(ToolEntity entity) {
        if (entity == null) {
            return false;
        }

        // 验证必填字段
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            return false;
        }

        if (entity.getDesc() == null) {
            return false;
        }

        // 验证长度限制
        if (entity.getName().length() > 100) { // 可以配置化
            return false;
        }

        if (entity.getDesc().length() > 500) { // 可以配置化
            return false;
        }

        if (entity.getCode() != null && entity.getCode().length() > 10000) { // 可以配置化
            return false;
        }

        // 验证工具类型
        if (entity.getToolType() != null) {
            boolean validType = ToolConstants.ToolType.CUSTOM.equals(entity.getToolType()) ||
                               ToolConstants.ToolType.MCP.equals(entity.getToolType());
            if (!validType) {
                return false;
            }
        }

        // 验证MCP配置
        if (ToolConstants.ToolType.MCP.equals(entity.getToolType())) {
            return validateMcpServerConfig(entity);
        }

        return true;
    }
}