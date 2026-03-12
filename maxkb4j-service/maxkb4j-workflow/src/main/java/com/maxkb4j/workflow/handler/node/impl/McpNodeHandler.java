package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.executor.McpClientExecutor;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.McpNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@NodeHandlerType(NodeType.MCP)
@RequiredArgsConstructor
@Component
public class McpNodeHandler implements INodeHandler {

    private final IToolService toolService;

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
        String resultText=new McpClientExecutor(nodeParams.getMcpServers()).execute(nodeParams.getMcpTool(),params);
        node.getDetail().put("toolParams",toolParams);
        node.getDetail().put("mcpTool",nodeParams.getMcpTool());
        return new NodeResult(Map.of("result",resultText));
    }
}
