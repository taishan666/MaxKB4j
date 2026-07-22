package com.maxkb4j.tool.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.annotation.ToolHandlerType;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.util.McpToolUtil;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for MCP tools. Each tool's code is an MCP servers config.
 */
@Component
@ToolHandlerType(ToolConstants.ToolType.MCP)
public class McpToolHandler extends AbsToolHandler {

    @Override
    public List<AiServiceTool> buildAiServiceTools(ToolEntity tool, String userMessage) {
        JSONObject mcpConfig = JSONObject.parseObject(tool.getCode());
        return McpToolUtil.getTools(mcpConfig);
    }

    @Override
    public List<ToolProvider> buildToolProviders(List<ToolEntity> tools) {
        List<ToolProvider> toolProviders = new ArrayList<>();
        for (ToolEntity tool : tools) {
            JSONObject mcpConfig = JSONObject.parseObject(tool.getCode());
            McpToolProvider mcpToolProvider = McpToolUtil.getMcpToolProvider(mcpConfig);
            if (mcpToolProvider != null) {
                toolProviders.add(mcpToolProvider);
            }
        }
        return toolProviders;
    }
}