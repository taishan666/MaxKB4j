package com.maxkb4j.tool.util;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.mcp.McpErrorCodePatch;
import com.maxkb4j.tool.mcp.SseHttpMcpTransport;
import com.maxkb4j.tool.vo.McpToolVO;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolExecutor;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class McpToolUtil {

    private static final Logger log = LoggerFactory.getLogger(McpToolUtil.class);

    /**
     * SSE 传输只存在于旧版协议（2026-07-28 之前），固定按 legacy initialize 握手。
     * 不指定协议版本时客户端会先发 server/discover 探测请求，旧版服务器无法识别该方法，
     * 可能直接以 4xx 拒绝导致握手失败。
     */
    private static final String LEGACY_PROTOCOL_VERSION = "2025-11-25";

    static {
        // 必须在首个 MCP 客户端使用 McpJson 反序列化之前应用
        McpErrorCodePatch.apply();
    }


    public static Map<ToolSpecification, ToolExecutor> getToolMap(JSONObject mcpServers) {
        Map<ToolSpecification, ToolExecutor> toolMap = new HashMap<>();
        forEachServerClient(mcpServers, (serverName, mcpClient) ->
                mcpClient.listTools().forEach(tool -> toolMap.put(tool, new McpToolExecutor(mcpClient))));
        return toolMap;
    }

    public static List<AiServiceTool> getTools(JSONObject mcpServers) {
        List<AiServiceTool> tools = new ArrayList<>();
        forEachServerClient(mcpServers, (serverName, mcpClient) ->
                mcpClient.listTools().forEach(tool -> tools.add(AiServiceTool.builder()
                        .toolSpecification(tool)
                        .toolExecutor(new McpToolExecutor(mcpClient))
                        .build())));
        return tools;
    }

    public static McpToolProvider getMcpToolProvider(JSONObject mcpServers) {
        List<McpClient> mcpClients = new ArrayList<>();
        forEachServerClient(mcpServers, (serverName, mcpClient) -> mcpClients.add(mcpClient));
        if (mcpClients.isEmpty()) {
            return null;
        }
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .build();
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
        // 可在服务器配置中显式指定协议版本（如 "2025-11-25"），跳过 server/discover 版本探测
        String protocolVersion = serverConfig.getString("protocolVersion");
        boolean hasProtocolVersion = protocolVersion != null && !protocolVersion.isBlank();
        McpTransport transport;
        DefaultMcpClient.Builder builder;
        if (ToolConstants.McpType.SSE.equalsIgnoreCase(type)) {
            // langchain4j-mcp 1.20.0-beta30 起移除了 SSE 传输，这里使用自定义实现
            transport = SseHttpMcpTransport.builder()
                    .url(url)
                    .customHeaders(headers)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            builder = new DefaultMcpClient.Builder()
                    .key(key)
                    .transport(transport)
                    // SSE 只存在于旧版协议，未显式配置时固定 legacy initialize
                    .protocolVersion(hasProtocolVersion ? protocolVersion : LEGACY_PROTOCOL_VERSION);
        } else {
            transport = StreamableHttpMcpTransport.builder()
                    .url(url)
                    .customHeaders(headers)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            builder = new DefaultMcpClient.Builder()
                    .key(key)
                    .transport(transport);
            if (hasProtocolVersion) {
                builder.protocolVersion(protocolVersion);
            }
        }
        // 这三项订阅默认开启，握手后自动发送 subscriptions/listen；未实现该方法的服务器
        // 会返回 404 并触发 "List-change subscription failed" 告警。本项目客户端均为
        // 短生命周期且不消费变更通知，显式关闭。
        builder.subscribeToToolListChanges(false)
                .subscribeToPromptListChanges(false)
                .subscribeToResourceListChanges(false);
        // 自动健康检查默认开启（30 秒一次），会为每个客户端常驻一个检查线程；客户端
        // 用完即弃不会被关闭，服务器会话过期后检查线程会反复重连失败刷告警
        // （"mcp server reconnection failed"），这里一并关闭。
        builder.autoHealthCheck(false);
        try {
            return builder.build();
        } catch (RuntimeException e) {
            // 客户端初始化失败时不会关闭传输层，这里主动关闭，避免 SSE 长连接泄漏
            closeQuietly(transport);
            throw e;
        }
    }

    public static List<McpToolVO> getToolVos(JSONObject mcpServers) {
        List<McpToolVO> toolVos = new ArrayList<>();
        forEachServerClient(mcpServers, (serverName, mcpClient) ->
                toolVos.addAll(convert(serverName, mcpClient.listTools())));
        return toolVos;
    }

    private interface ServerClientConsumer {

        void accept(String serverName, McpClient mcpClient) throws Exception;
    }

    /**
     * 遍历 mcpServers 中的所有服务器逐个建连。单个服务器不可用（网络异常、
     * 协议握手失败如 410 等）只记录告警并跳过，不影响其余服务器，
     * 避免一个失效的 MCP 配置拖垮整个对话。
     */
    private static void forEachServerClient(JSONObject mcpServers, ServerClientConsumer consumer) {
        if (mcpServers == null || mcpServers.isEmpty()) {
            return;
        }
        for (String key : mcpServers.keySet()) {
            JSONObject serverConfig = mcpServers.getJSONObject(key);
            if (serverConfig == null) {
                continue;
            }
            try {
                McpClient mcpClient = getMcpClient(key, serverConfig);
                consumer.accept(key, mcpClient);
            } catch (Exception e) {
                log.warn("MCP server [{}] connection failed, skip it: {}", key, rootMessage(e), e);
            }
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? t.toString() : cause.getMessage();
    }

    private static void closeQuietly(McpTransport transport) {
        try {
            transport.close();
        } catch (Exception ignore) {
        }
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
                switch (v) {
                    case JsonStringSchema schema -> {
                        property.put("type", "string");
                        property.put("description", schema.description());
                    }
                    case JsonNumberSchema schema -> {
                        property.put("type", "number");
                        property.put("description", schema.description());
                    }
                    case JsonArraySchema schema -> {
                        property.put("type", "array");
                        property.put("description", schema.description());
                    }
                    case JsonBooleanSchema schema -> {
                        property.put("type", "boolean");
                        property.put("description", schema.description());
                    }
                    case JsonObjectSchema schema -> {
                        property.put("type", "object");
                        property.put("description", schema.description());
                    }
                    case JsonEnumSchema schema -> {
                        property.put("type", "enum");
                        property.put("description", schema.description());
                    }
                    case JsonIntegerSchema schema -> {
                        property.put("type", "int");
                        property.put("description", schema.description());
                    }
                    case JsonAnyOfSchema schema -> {
                        property.put("type", "any");
                        property.put("description", schema.description());
                    }
                    case JsonReferenceSchema schema -> {
                        property.put("type", "reference");
                        property.put("description", schema.reference());
                    }
                    case null, default -> {
                        property.put("type", "null");
                        property.put("description", "");
                    }
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
