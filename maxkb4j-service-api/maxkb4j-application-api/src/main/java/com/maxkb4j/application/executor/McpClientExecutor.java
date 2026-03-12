package com.maxkb4j.application.executor;

import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.service.tool.ToolExecutionResult;

import java.util.*;

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
            McpClient mcpClient = getMcpClient(key,serverConfig);
            ToolExecutionRequest toolExecutionRequest=ToolExecutionRequest.builder()
                    .name(toolName)
                    .arguments(params.toJSONString())
                    .build();
            ToolExecutionResult toolExecutionResult=mcpClient.executeTool(toolExecutionRequest);
            return toolExecutionResult.resultText();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static McpClient getMcpClient(String key,JSONObject serverConfig) {
        String url = serverConfig.getString("url");
        String type = serverConfig.getString("type");
        Map<String, String> headers =new HashMap<>();
        if (serverConfig.containsKey("headers")) {
            headers = (Map<String, String>) serverConfig.get("headers");
        }
        McpTransport transport;
        if ("sse".equals(type)) {
            transport = new HttpMcpTransport.Builder()
                    .sseUrl(url)
                    .customHeaders(headers)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        } else {
            transport = StreamableHttpMcpTransport.builder()
                    .url(url)
                    .customHeaders(headers)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        }
        return new DefaultMcpClient.Builder()
                .key(key)
                .transport(transport)
                .build();
    }
}
