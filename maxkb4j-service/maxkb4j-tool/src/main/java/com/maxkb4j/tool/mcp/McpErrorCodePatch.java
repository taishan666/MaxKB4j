package com.maxkb4j.tool.mcp;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.langchain4j.mcp.client.transport.McpJson;
import dev.langchain4j.mcp.protocol.McpErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * langchain4j（1.20.0-beta30）把 JSON-RPC 错误码反序列化为 int，但部分服务器
 * 会返回超出 int 范围的 code（如火山方舟内容审核的 10990201005），导致整个错误
 * 响应反序列化失败，LLM 只能看到 Jackson 异常文本而不是服务器返回的真实错误信息。
 * 这里向 McpJson 内部的 ObjectMapper 注册宽松的 Error 反序列化器：
 * code 按 long 读取后窄化为 int（仅用于展示），message/data 原样保留。
 */
public final class McpErrorCodePatch {

    private static final Logger log = LoggerFactory.getLogger(McpErrorCodePatch.class);
    private static final AtomicBoolean APPLIED = new AtomicBoolean(false);

    private McpErrorCodePatch() {
    }

    public static void apply() {
        if (!APPLIED.compareAndSet(false, true)) {
            return;
        }
        try {
            Field codecField = McpJson.class.getDeclaredField("CODEC");
            codecField.setAccessible(true);
            Object codec = codecField.get(null);
            Field mapperField = codec.getClass().getDeclaredField("objectMapper");
            mapperField.setAccessible(true);
            ObjectMapper mapper = (ObjectMapper) mapperField.get(codec);

            SimpleModule module = new SimpleModule("maxkb4j-mcp-error-code");
            module.addDeserializer(McpErrorResponse.Error.class, new ErrorDeserializer(mapper));
            mapper.registerModule(module);
        } catch (Exception e) {
            log.warn("修补 McpJson 错误码反序列化失败，超出 int 范围的 JSON-RPC 错误码仍可能导致反序列化异常", e);
        }
    }

    private static final class ErrorDeserializer extends StdDeserializer<McpErrorResponse.Error> {

        private final ObjectMapper mapper;

        ErrorDeserializer(ObjectMapper mapper) {
            super(McpErrorResponse.Error.class);
            this.mapper = mapper;
        }

        @Override
        public McpErrorResponse.Error deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = (JsonNode) p.getCodec().readTree(p);
            int code = (int) node.path("code").asLong(0);
            String message = node.hasNonNull("message") ? node.get("message").asText() : null;
            Object data = node.hasNonNull("data") ? mapper.treeToValue(node.get("data"), Object.class) : null;
            return new McpErrorResponse.Error(code, message, data);
        }
    }
}
