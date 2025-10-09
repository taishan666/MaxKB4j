package com.tarzan.maxkb4j.core.workflow.node.mcp.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.mcp.input.McpParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;

import java.util.ArrayList;
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
        for (String key : toolParams.keySet()) {
            Object value =toolParams.get(key);
            if (value instanceof List){
                @SuppressWarnings("unchecked")
                List<String> fields=(List<String>)value;
                value=super.getReferenceField(fields.get(0),fields.get(1));
            }
            params.put(key,value);
        }

        JSONObject mcpServers=JSONObject.parseObject(nodeParams.getMcpServers());
        List<McpClient> mcpClients = McpToolUtil.getMcpClients(mcpServers);
        List<String> result=new ArrayList<>();
        for (McpClient mcpClient : mcpClients) {
            ToolExecutionRequest toolExecutionRequest=ToolExecutionRequest.builder()
                    .name(nodeParams.getMcpTool())
                    .arguments(params.toJSONString())
                    .build();
            result.add(mcpClient.executeTool(toolExecutionRequest));
        }
        return new NodeResult(Map.of("result",result,"toolParams",toolParams,"mcpTool",nodeParams.getMcpTool()), Map.of());
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }

    @Override
    public JSONObject getRunDetail() {
        return detail;
    }

}
