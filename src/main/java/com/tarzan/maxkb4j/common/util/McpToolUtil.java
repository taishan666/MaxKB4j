package com.tarzan.maxkb4j.common.util;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domain.vo.McpToolVO;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolExecutor;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class McpToolUtil {


    public static Map<ToolSpecification, ToolExecutor> getToolMap(JSONObject mcpServers) {
        Map<ToolSpecification, ToolExecutor> mcpToolMap = new HashMap<>();
        mcpServers.keySet().forEach(key -> {
            JSONObject serverConfig = (JSONObject) mcpServers.get(key);
            McpClient mcpClient = getMcpClient(key,serverConfig);
            Map<ToolSpecification, ToolExecutor> mcpClientToolMap = mcpClient.listTools().stream().collect(Collectors.toMap(
                    mcpTool -> mcpTool,
                    mcpTool -> new McpToolExecutor(mcpClient)
            ));
            mcpToolMap.putAll(mcpClientToolMap);
        });
        return mcpToolMap;
    }

    public static List<McpClient> getMcpClients(JSONObject mcpServers) {
        List<McpClient> mcpClients = new ArrayList<>();
        mcpServers.keySet().forEach(key -> {
            JSONObject serverConfig = (JSONObject) mcpServers.get(key);
            McpClient mcpClient = getMcpClient(key,serverConfig);
            mcpClients.add(mcpClient);
        });
        return mcpClients;
    }

    @SuppressWarnings("unchecked")
    private static McpClient getMcpClient(String key,JSONObject serverConfig) {
        String url = serverConfig.getString("url");
        StreamableHttpMcpTransport.Builder builder = new StreamableHttpMcpTransport.Builder();
        builder.url(url).logRequests(true).logResponses(true);
        if (serverConfig.containsKey("headers")) {
            Map<String, String> headers = (Map<String, String>) serverConfig.get("headers");
            builder.customHeaders(headers);
        }
        McpTransport transport = builder.build();
        return new DefaultMcpClient.Builder()
                .key(key)
                .transport(transport)
                .build();
    }

    public static List<McpToolVO> getToolVos(JSONObject mcpServers) {
        List<McpToolVO> toolVos = new ArrayList<>();
        mcpServers.keySet().forEach(key -> {
            JSONObject serverConfig = (JSONObject) mcpServers.get(key);
            McpClient mcpClient = getMcpClient(key,serverConfig);
            toolVos.addAll(convert(key, mcpClient.listTools()));
        });
        return toolVos;
    }

    private static List<McpToolVO> convert(String serverName, List<ToolSpecification> tools) {
        return tools.stream().map(tool -> {
            McpToolVO vo = new McpToolVO();
            vo.setServer(serverName);
            vo.setName(tool.name());
            vo.setDescription(tool.description());
            JSONObject json = new JSONObject();
            JSONObject properties = new JSONObject();
            tool.parameters().properties().forEach((k, v) -> {
                JSONObject property = new JSONObject();
                if (v instanceof JsonStringSchema schema) {
                    property.put("type", "string");
                    property.put("description", schema.description());
                } else if (v instanceof JsonNumberSchema schema) {
                    property.put("type", "number");
                    property.put("description", schema.description());
                } else if (v instanceof JsonArraySchema schema) {
                    property.put("type", "array");
                    property.put("description", schema.description());
                } else if (v instanceof JsonBooleanSchema schema) {
                    property.put("type", "boolean");
                    property.put("description", schema.description());
                } else if (v instanceof JsonObjectSchema schema) {
                    property.put("type", "object");
                    property.put("description", schema.description());
                } else if (v instanceof JsonEnumSchema schema) {
                    property.put("type", "enum");
                    property.put("description", schema.description());
                } else if (v instanceof JsonIntegerSchema schema) {
                    property.put("type", "int");
                    property.put("description", schema.description());
                } else if (v instanceof JsonAnyOfSchema schema) {
                    property.put("type", "any");
                    property.put("description", schema.description());
                } else if (v instanceof JsonReferenceSchema schema) {
                    property.put("type", "reference");
                    property.put("description", schema.reference());
                } else {
                    property.put("type", "null");
                    property.put("description", "");
                }
                properties.put(k, property);
            });
            json.put("type", "object");
            json.put("properties", properties);
            json.put("required", tool.parameters().required());
            vo.setArgs_schema(json);
            return vo;
        }).collect(Collectors.toList());
    }
}
