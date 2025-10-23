package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.mcp.impl.McpNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Component
public class McpNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        McpNode.NodeParams nodeParams=node.getNodeData().toJavaObject(McpNode.NodeParams.class);
        JSONObject toolParams=nodeParams.getToolParams();
        JSONObject params=new JSONObject();
        for (String key : toolParams.keySet()) {
            Object value =toolParams.get(key);
            if (value instanceof List){
                @SuppressWarnings("unchecked")
                List<String> fields=(List<String>)value;
                value=workflow.getReferenceField(fields.get(0),fields.get(1));
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
            ToolExecutionResult toolExecutionResult=mcpClient.executeTool(toolExecutionRequest);
            result.add(toolExecutionResult.resultText());
        }
        node.getDetail().put("toolParams",toolParams);
        node.getDetail().put("mcpTool",nodeParams.getMcpTool());
        return new NodeResult(Map.of("result",result), Map.of());
    }
}
