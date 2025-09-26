package com.tarzan.maxkb4j.core.workflow.node.mcp.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.mcp.input.McpParams;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.MCP;

public class McpNode extends INode {

    public McpNode(JSONObject properties) {
        super(properties);
        this.type=MCP.getKey();
    }

    @Override
    public NodeResult execute() {
        McpParams nodeParams=super.getNodeData().toJavaObject(McpParams.class);
        JSONObject toolParams=nodeParams.getToolParams();
        JSONObject params=new JSONObject();
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(nodeParams.getSseUrl())
                .build();
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
        ToolSpecification toolSpecification=mcpClient.listTools().stream().filter(e->e.name().equals(nodeParams.getMcpTool())).findFirst().get();
        JsonObjectSchema jsonObjectSchema =toolSpecification.parameters();
        jsonObjectSchema.properties().forEach((k,v)->{
            Object value =toolParams.get(k);
            if (value instanceof List){
                @SuppressWarnings("unchecked")
                List<String> fields=(List<String>)value;
                value=super.getReferenceField(fields.get(0),fields.get(1));
            }
            params.put(k,value);
        });
        ToolExecutionRequest toolExecutionRequest=ToolExecutionRequest.builder()
                .name(toolSpecification.name())
                .arguments(params.toJSONString())
                .build();
        String result=mcpClient.executeTool(toolExecutionRequest);
        return new NodeResult(Map.of("result",result,"toolParams",toolParams,"mcpTool",nodeParams.getMcpTool()), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("mcpTool", context.get("mcpTool"));
        detail.put("toolParams", context.get("toolParams"));
        detail.put("result", context.get("result"));
        return detail;
    }

}
