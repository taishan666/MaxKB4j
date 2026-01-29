package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.McpNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@NodeHandlerType(NodeType.MCP)
@RequiredArgsConstructor
@Component
public class McpNodeHandler implements INodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        McpNode.NodeParams nodeParams=node.getNodeData().toJavaObject(McpNode.NodeParams.class);
        JSONObject toolParams=nodeParams.getToolParams();
        JSONObject params=new JSONObject();
        for (String key : toolParams.keySet()) {
            Object value =toolParams.get(key);
            if (value instanceof List){
                List<String> fields=(List<String>)value;
                value=workflow.getReferenceField(fields);
            }
            params.put(key,value);
        }
        JSONObject mcpServers=JSONObject.parseObject(nodeParams.getMcpServers());
        List<McpClient> mcpClients = McpToolUtil.getMcpClients(mcpServers);
        Optional<McpClient> opt=mcpClients.stream().filter(mcpClient -> mcpClient.listTools().stream().anyMatch(tool -> tool.name().equals(nodeParams.getMcpTool()))).findFirst();
        ToolExecutionRequest toolExecutionRequest=ToolExecutionRequest.builder()
                .name(nodeParams.getMcpTool())
                .arguments(params.toJSONString())
                .build();
        String resultText="";
        if (opt.isPresent()){
            ToolExecutionResult toolExecutionResult=opt.get().executeTool(toolExecutionRequest);
            resultText=toolExecutionResult.resultText();
        }
        node.getDetail().put("toolParams",toolParams);
        node.getDetail().put("mcpTool",nodeParams.getMcpTool());
        return new NodeResult(Map.of("result",resultText));
    }
}
