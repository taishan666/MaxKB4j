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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class McpToolUtil {

    private static final Logger log = LoggerFactory.getLogger(McpToolUtil.class);

    /**
     * SSE 传输只存在于旧版协议（2026-07-28 之前），固定按 legacy initialize 握手。
     * 不指定协议版本时客户端会先发 server/discover 探测请求，旧版服务器无法识别该方法，
     * 可能直接以 4xx 拒绝导致握手失败。
     */
    private static final String LEGACY_PROTOCOL_VERSION = "2025-11-25";
    /**
     * 模型侧函数名上限（OpenAI 兼容约定为 64，且仅允许 [a-zA-Z0-9_-]）。
     */
    private static final int MAX_TOOL_NAME_LENGTH = 64;

    /**
     * 缓存空闲超过该时长的客户端在下次访问时关闭驱逐，避免服务器会话过期后残留死连接。
     */
    private static final long CLIENT_IDLE_CLOSE_MS = 10 * 60 * 1000L;

    /**
     * MCP 客户端复用缓存：key 为服务器配置指纹，value 为客户端与最近访问时间。
     * 每次会话都新建客户端（网络握手 + 常驻线程）且从不关闭会导致连接与线程泄漏，
     * 相同配置的客户端在空闲超时内跨会话复用。
     */
    private static final Map<String, CachedClient> CLIENT_CACHE = new ConcurrentHashMap<>();

    private record CachedClient(McpClient client, AtomicLong lastAccess) {
    }

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

    public static McpToolProvider getMcpToolProvider(String toolId, JSONObject mcpServers) {
        List<McpClient> mcpClients = new ArrayList<>();
        forEachServerClient(mcpServers, (serverName, mcpClient) -> mcpClients.add(mcpClient));
        if (mcpClients.isEmpty()) {
            return null;
        }
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                // 一个工具实体（toolId）可能对应服务端暴露的多个工具，若统一映射为
                // tool_<id> 会触发 langchain4j "Duplicated definition for tool"。
                // 这里在基础名后追加由「服务器 key + 原始工具名」派生的唯一后缀，
                // 既保证名称唯一，又能被 ToolNaming.parse 还原回 toolId 用于展示。
                .toolNameMapper((client, toolSpec) ->
                        buildMcpToolName(toolId, client.key(), toolSpec.name()))
                .build();
    }

    /**
     * 构建 MCP 工具的唯一调用名：tool_&lt;id&gt;__&lt;suffix&gt;。
     * suffix 由服务器 key 与原始工具名清洗拼接而成，超长时截断并追加短哈希避免碰撞。
     */
    static String buildMcpToolName(String toolId, String clientKey, String originalName) {
        String key = clientKey == null ? "" : clientKey;
        String name = originalName == null ? "" : originalName;
        String base = ToolNaming.buildToolName(toolId);
        int budget = MAX_TOOL_NAME_LENGTH - base.length() - ToolNaming.SUFFIX_SEPARATOR.length();
        String suffix;
        if (budget <= 0) {
            // id 过长的极端情况，直接用短哈希保证唯一
            suffix = shortHash(key + "/" + name);
        } else {
            suffix = sanitizeNameSuffix(key + "_" + name);
            if (suffix.length() > budget) {
                String hash = shortHash(key + "/" + name);
                int keep = Math.max(0, budget - hash.length() - 1);
                suffix = suffix.substring(0, keep) + "_" + hash;
            }
        }
        return ToolNaming.buildToolName(toolId, suffix);
    }

    /**
     * 将任意字符串清洗为合法的函数名后缀（仅保留 [a-zA-Z0-9_-]，其余替换为下划线）。
     */
    private static String sanitizeNameSuffix(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '-';
            sb.append(valid ? c : '_');
        }
        return sb.toString();
    }

    /**
     * 取 MD5 前 12 位十六进制作为短哈希，用于超长名称截断后仍保证唯一性。
     */
    private static String shortHash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    public static McpClient getMcpClient(JSONObject mcpServers) {
        Optional<String> keyOpt = mcpServers.keySet().stream().findFirst();
        if (keyOpt.isPresent()) {
            String key = keyOpt.get();
            JSONObject serverConfig = mcpServers.getJSONObject(key);
            return getOrCreateClient(key, serverConfig);
        }
        return null;
    }

    /**
     * 获取（或创建并缓存）指定服务器配置的 MCP 客户端。
     * 相同配置的客户端在空闲超时内复用；调用失败可通过 {@link #evict(McpClient)} 驱逐。
     */
    private static McpClient getOrCreateClient(String key, JSONObject serverConfig) {
        evictIdleClients();
        String fingerprint = key + "|" + serverConfig.toJSONString();
        while (true) {
            CachedClient cached = CLIENT_CACHE.get(fingerprint);
            if (cached != null) {
                cached.lastAccess().set(System.currentTimeMillis());
                return cached.client();
            }
            McpClient created = buildClient(key, serverConfig);
            CachedClient newcomer = new CachedClient(created, new AtomicLong(System.currentTimeMillis()));
            CachedClient existing = CLIENT_CACHE.putIfAbsent(fingerprint, newcomer);
            if (existing == null) {
                return created;
            }
            // 并发下其他线程已创建相同配置的客户端，关闭本次多余实例
            closeQuietly(created);
            existing.lastAccess().set(System.currentTimeMillis());
            return existing.client();
        }
    }

    /**
     * 驱逐调用失败的客户端，下次访问将重建连接，避免复用已失效的连接/会话。
     */
    public static void evict(McpClient client) {
        CLIENT_CACHE.entrySet().removeIf(entry -> entry.getValue().client() == client);
        closeQuietly(client);
    }

    private static void evictIdleClients() {
        long now = System.currentTimeMillis();
        CLIENT_CACHE.entrySet().removeIf(entry -> {
            if (now - entry.getValue().lastAccess().get() > CLIENT_IDLE_CLOSE_MS) {
                log.info("Closing idle MCP client for server [{}]", entry.getKey());
                closeQuietly(entry.getValue().client());
                return true;
            }
            return false;
        });
    }

    @SuppressWarnings("unchecked")
    private static McpClient buildClient(String key, JSONObject serverConfig) {
        String url = serverConfig.getString("url");
        String type = serverConfig.getString("type");
        Map<String, String> headers = new HashMap<>();
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
        // 会被缓存复用但不消费变更通知，服务器会话过期后检查线程会反复重连失败刷告警
        // （"mcp server reconnection failed"），这里一并关闭，失效连接由调用方 evict 驱逐重建。
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

    /**
     * 遍历 mcpServers 中的所有服务器逐个获取客户端（带缓存复用）。单个服务器不可用
     * （网络异常、协议握手失败如 410 等）只记录告警并跳过，不影响其余服务器，
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
                McpClient mcpClient = getOrCreateClient(key, serverConfig);
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

    private static void closeQuietly(McpClient client) {
        try {
            client.close();
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

    private interface ServerClientConsumer {

        void accept(String serverName, McpClient mcpClient) throws Exception;
    }
}
