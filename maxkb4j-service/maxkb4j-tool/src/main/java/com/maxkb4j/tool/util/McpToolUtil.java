package com.maxkb4j.tool.util;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.vo.McpToolVO;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolExecutor;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.*;
import java.util.stream.Collectors;

public class McpToolUtil {


    public static Map<ToolSpecification, ToolExecutor> getToolMap(JSONObject mcpServers) {
        McpClient mcpClient = getMcpClient(mcpServers);
        if (Objects.nonNull(mcpClient)){
            return mcpClient.listTools().stream().collect(Collectors.toMap(
                    mcpTool -> mcpTool,
                    mcpTool -> new McpToolExecutor(mcpClient)
            ));
        }
        return Map.of();
    }

    public static McpClient getMcpClient(JSONObject mcpServers) {
        Optional<String> keyOpt = mcpServers.keySet().stream().findFirst();
        if (keyOpt.isPresent()){
            String key = keyOpt.get();
            JSONObject serverConfig = mcpServers.getJSONObject(key);
            return getMcpClient(key,serverConfig);
        }
        return null;
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
