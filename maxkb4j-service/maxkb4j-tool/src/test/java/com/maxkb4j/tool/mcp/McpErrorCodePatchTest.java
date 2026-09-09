package com.maxkb4j.tool.mcp;

import dev.langchain4j.mcp.client.transport.McpJson;
import dev.langchain4j.mcp.protocol.McpErrorResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpErrorCodePatchTest {

    @Test
    void shouldDeserializeErrorCodeOutOfRangeOfInt() {
        McpErrorCodePatch.apply();

        String json = "{\"jsonrpc\":\"2.0\",\"id\":3,\"error\":{" +
                "\"code\":10990201005," +
                "\"message\":\"当前输出可能包含不当内容，请修改输入后再试。 Request ID: 106b0495-497c-4c4a-bc02-e7dcd08ad199\"}}";

        McpErrorResponse response = McpJson.deserialize(json, McpErrorResponse.class);

        McpErrorResponse.Error error = response.getError();
        // code 按 long 读取后窄化为 int（10990201005 & 0xFFFFFFFF 的有符号结果）
        assertEquals((int) 10990201005L, error.getCode());
        assertTrue(error.getMessage().contains("当前输出可能包含不当内容"));
    }

    @Test
    void shouldDeserializeStandardRangeErrorCode() {
        McpErrorCodePatch.apply();

        String json = "{\"jsonrpc\":\"2.0\",\"id\":3,\"error\":{\"code\":-32602,\"message\":\"Invalid params\"}}";

        McpErrorResponse response = McpJson.deserialize(json, McpErrorResponse.class);

        McpErrorResponse.Error error = response.getError();
        assertEquals(-32602, error.getCode());
        assertEquals("Invalid params", error.getMessage());
    }
}
