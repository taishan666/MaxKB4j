package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.service.IToolExecuteService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.McpNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@Slf4j
@NodeHandlerType(NodeType.MCP)
@RequiredArgsConstructor
@Component
public class McpNodeHandler extends AbsNodeHandler {

    private final IToolExecuteService toolExecuteService;

    @Override
    @SuppressWarnings("unchecked")
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        McpNode.NodeParams params = parseParams(node, McpNode.NodeParams.class);
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
        String resultText =toolExecuteService.mcpToolExecute(params.getMcpServers(),params.getMcpTool(), execParams);
        putDetails(node, Map.of(
                ToolField.TOOL_PARAMS, execParams,
                ToolField.MCP_TOOL, params.getMcpTool()
        ));

        return new NodeResult(Map.of(NodeField.RESULT, List.of(resultText)));
    }
}
