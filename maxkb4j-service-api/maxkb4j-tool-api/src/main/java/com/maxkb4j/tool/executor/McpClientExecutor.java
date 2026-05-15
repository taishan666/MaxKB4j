package com.maxkb4j.tool.executor;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.util.McpToolUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;

import java.util.Optional;

public class McpClientExecutor {

    private final JSONObject mcpServers;

    public McpClientExecutor(String code) {
        this.mcpServers=JSONObject.parseObject(code);
    }

    public String execute(String toolName,JSONObject params) {
        Optional<String> keyOpt = mcpServers.keySet().stream().findFirst();
        if (keyOpt.isPresent()){
            String key = keyOpt.get();
            JSONObject serverConfig = mcpServers.getJSONObject(key);
            McpClient mcpClient = McpToolUtil.getMcpClient(key,serverConfig);
            ToolExecutionRequest toolExecutionRequest=ToolExecutionRequest.builder()
                    .name(toolName)
                    .arguments(params.toJSONString())
                    .build();
            ToolExecutionResult toolExecutionResult=mcpClient.executeTool(toolExecutionRequest);
            try {
                mcpClient.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return toolExecutionResult.resultText();
        }
        return "";
    }

}
