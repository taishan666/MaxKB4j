package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.executor.McpClientExecutor;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
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
public class McpNodeHandler extends AbstractNodeHandler<McpNode.NodeParams> {

    private final IToolService toolService;

    @Override
    protected Class<McpNode.NodeParams> getParamsClass() {
        return McpNode.NodeParams.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NodeResult doExecute(Workflow workflow, AbsNode node, McpNode.NodeParams params) throws Exception {
        JSONObject toolParams = params.getToolParams();
        JSONObject execParams = new JSONObject();

        for (String key : toolParams.keySet()) {
            Object value = toolParams.get(key);
            if (value instanceof List) {
                List<String> fields = (List<String>) value;
                value = workflow.getReferenceField(fields);
            }
            execParams.put(key, value);
        }

        String resultText = new McpClientExecutor(params.getMcpServers()).execute(params.getMcpTool(), execParams);

        putDetails(node, Map.of(
                "toolParams", toolParams,
                "mcpTool", params.getMcpTool()
        ));

        return new NodeResult(Map.of("result", resultText));
    }
}
