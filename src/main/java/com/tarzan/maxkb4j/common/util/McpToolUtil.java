package com.tarzan.maxkb4j.common.util;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domian.vo.McpToolVO;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolExecutor;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class McpToolUtil {


    public static List<ToolSpecification> getTools(JSONObject mcpServers) {
        List<ToolSpecification> tools = new ArrayList<>();
        mcpServers.keySet().forEach(key -> {
            JSONObject serverConfig = (JSONObject) mcpServers.get(key);
            McpClient mcpClient = getMcpClient(serverConfig);
            tools.addAll(mcpClient.listTools());
        });
        return tools;
    }

    public static Map<ToolSpecification, ToolExecutor> getToolMap(JSONObject mcpServers) {
        Map<ToolSpecification, ToolExecutor> mcpToolMap = new HashMap<>();
        mcpServers.keySet().forEach(key -> {
            JSONObject serverConfig = (JSONObject) mcpServers.get(key);
            McpClient mcpClient = getMcpClient(serverConfig);
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
            McpClient mcpClient = getMcpClient(serverConfig);
            mcpClients.add(mcpClient);
        });
        return mcpClients;
    }

    public static McpClient getMcpClient(JSONObject serverConfig) {
        String url = serverConfig.getString("url");
        String type = serverConfig.getString("type");
        McpTransport transport;
        if ("sse".equals(type)) {
            transport = new HttpMcpTransport.Builder()
                    .sseUrl(url)
                    .logRequests(true) // if you want to see the traffic in the log
                    .logResponses(true)
                    .build();
        } else {
            transport = new StreamableHttpMcpTransport.Builder()
                    .url(url)
                    .logRequests(true) // if you want to see the traffic in the log
                    .logResponses(true)
                    .build();
        }
        return new DefaultMcpClient.Builder()
                .key("MaxKB4JMCPClient")
                .transport(transport)
                .build();
    }

    public static List<McpToolVO> getToolVos(JSONObject mcpServers) {
        List<McpToolVO> tools = new ArrayList<>();
        mcpServers.keySet().forEach(server -> {
            JSONObject serverConfig = (JSONObject) mcpServers.get(server);
            McpClient mcpClient = getMcpClient(serverConfig);
            tools.addAll(convert(server, mcpClient.listTools()));
        });
        return tools;
    }

    public static List<McpToolVO> convert(String serverName, List<ToolSpecification> tools) {
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
                    JsonNullSchema schema = (JsonNullSchema) v;
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
