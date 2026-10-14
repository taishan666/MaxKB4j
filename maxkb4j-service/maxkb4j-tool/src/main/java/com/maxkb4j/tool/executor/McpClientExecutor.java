package com.maxkb4j.tool.executor;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.tool.util.McpToolUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;

public class McpClientExecutor {

    private final JSONObject mcpServers;

    public McpClientExecutor(String code) {
        this.mcpServers = JSONObject.parseObject(code);
    }

    public String execute(String toolName, JSONObject params) {
        McpClient mcpClient = McpToolUtil.getMcpClient(mcpServers);
        if (mcpClient == null) {
            throw new ApiException("tool.not.found");
        }
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .name(toolName)
                .arguments(params.toJSONString())
                .build();
        try {
            ToolExecutionResult toolExecutionResult = mcpClient.executeTool(toolExecutionRequest);
            return toolExecutionResult.resultText();
        } catch (RuntimeException e) {
            // 客户端由 McpToolUtil 缓存复用，此处不能关闭；调用失败（如连接/会话过期）
            // 时从缓存驱逐，下次调用重建连接
            McpToolUtil.evict(mcpClient);
            throw e;
        }
    }

}
