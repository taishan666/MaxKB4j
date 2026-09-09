package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.tool.service.IToolExecuteService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ToolNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;

@NodeHandlerType({NodeType.TOOL, NodeType.TOOL_LIB})
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolNodeHandler extends AbsNodeHandler {

    private final IToolExecuteService toolExecuteService;

    @Override
    @SuppressWarnings("unchecked")
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        ToolNode.NodeParams params = parseParams(node, ToolNode.NodeParams.class);
        Map<String, Object> inputParams = toolExecuteService.convertParamType(params.getInputFieldList());
        Object result=toolExecuteService.httpOrCodeExecute(params.getToolType(),params.getCode(),params.getInitParams(),inputParams);
        // 使用辅助方法写入详情
        putDetail(node, NodeField.PARAMS, inputParams);
        if (Boolean.TRUE.equals(params.getIsResult())) {
            setAnswerText(node, result.toString());
        }
        Map<String, Object> nodeVariable=new HashMap<>();
        if (result instanceof Map<?,?> resultMap){
            nodeVariable.putAll((Map<? extends String, ?>) resultMap);
        }
        nodeVariable.put(NodeField.RESULT,result);
        return new NodeResult(nodeVariable);
    }

}
