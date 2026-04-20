package com.maxkb4j.tool.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.exception.ToolValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
            if (!JSONUtil.isTypeJSONObject(jsonStr)){
                throw new ToolValidationException("MCP服务器配置解析失败");
            }
            JSONObject root= JSONUtil.parseObj(jsonStr);
            for (String fieldName : root.keySet()) {
                Object valueObj = root.get(fieldName);
                // 每个值必须是 JSONObject (对应原代码的 isObject())
                if (!(valueObj instanceof JSONObject value)) {
                    return false;
                }
                // 检查是否存在 url 和 type 字段
                if (!value.containsKey("url") || !value.containsKey("type")) {
                    return false;
                }
                Object urlObj = value.get("url");
                Object typeObj = value.get("type");
                // url 必须是非空字符串
                if (!(urlObj instanceof String urlStr)) {
                    return false;
                }
                if (urlStr.trim().isEmpty()) {
                    return false;
                }
                // type 必须是 "streamable_http" 或 "sse"
                if (!(typeObj instanceof String typeStr)) {
                    return false;
                }
                boolean supported = "streamable_http".equals(typeStr) || "sse".equals(typeStr);
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