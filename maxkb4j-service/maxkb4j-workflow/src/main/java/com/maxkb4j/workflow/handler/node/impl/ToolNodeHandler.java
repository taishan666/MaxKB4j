package com.maxkb4j.workflow.handler.node.impl;

import cn.hutool.http.HttpResponse;
import com.maxkb4j.tool.dto.ToolInputField;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.service.IToolExecuteService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ToolNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType({NodeType.TOOL, NodeType.TOOL_LIB})
@Component
@RequiredArgsConstructor
public class ToolNodeHandler extends AbsNodeHandler {

    private final IToolExecuteService toolExecuteService;

    @Override
    @SuppressWarnings("unchecked")
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        ToolNode.NodeParams params = parseParams(node, ToolNode.NodeParams.class);
        Map<String, Object> execParams = new HashMap<>(5);
        if (!CollectionUtils.isEmpty(params.getInputFieldList())) {
            for (ToolInputField inputField : params.getInputFieldList()) {
                Object value = workflow.getFieldValue(inputField.getValue(), inputField.getSource());
                execParams.put(inputField.getName(), value);
            }
        }
        Object result;
        if (ToolConstants.ToolType.HTTP.equals(params.getToolType())){
            HttpResponse httpResponse = toolExecuteService.httpExecute(params.getCode(),execParams);
            result = httpResponse.body();
        }else {
            result = toolExecuteService.customExecute(params.getCode(), params.getInitParams(),execParams);
        }
        // 使用辅助方法写入详情
        putDetail(node, "params", execParams);
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
