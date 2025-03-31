package com.tarzan.maxkb4j.core.workflow.node.mcp.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.mcp.IMcpNode;
import com.tarzan.maxkb4j.core.workflow.node.mcp.input.McpParams;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.List;
import java.util.Map;

public class BaseMcpNode extends IMcpNode {
    @Override
    public NodeResult execute(McpParams nodeParams, FlowParams workflowParams) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")
                .logRequests(true) // if you want to see the traffic in the log
                .logResponses(true)
                .build();
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();
        return new NodeResult(Map.of("result",context.get("result")), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("mcp_tool", context.get("mcp_tool"));
        detail.put("tool_params", context.get("tool_params"));
        detail.put("result", context.get("result"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
