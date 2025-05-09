package com.tarzan.maxkb4j.core.workflow.node.mcp.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.mcp.input.McpParams;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.MCP;

public class BaseMcpNode extends INode {

    public BaseMcpNode() {
        super();
        this.type=MCP.getKey();
    }

    @Override
    public NodeResult execute() {
        McpParams nodeParams=super.nodeParams.toJavaObject(McpParams.class);
        JSONObject params=new JSONObject();
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(nodeParams.getSseUrl())
                .build();
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
        ToolSpecification toolSpecification=mcpClient.listTools().get(0);
        JsonObjectSchema jsonObjectSchema =toolSpecification.parameters();
        jsonObjectSchema.properties().forEach((k,v)->{
            if(v instanceof JsonObjectSchema schema){
                params.put(k,schema.properties().get("value").toString());
            }
        });
        ToolExecutionRequest toolExecutionRequest=ToolExecutionRequest.builder()
                .id("1")
                .name(toolSpecification.name())
                .arguments(params.toJSONString())
                .build();
        String result=mcpClient.executeTool(toolExecutionRequest);
        return new NodeResult(Map.of("result",result), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("mcp_tool", context.get("mcp_tool"));
        detail.put("tool_params", context.get("tool_params"));
        detail.put("result", context.get("result"));
        return detail;
    }

}
