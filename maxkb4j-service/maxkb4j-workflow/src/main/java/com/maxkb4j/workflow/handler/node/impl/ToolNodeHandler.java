package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.application.executor.GroovyScriptExecutor;
import com.maxkb4j.common.mp.entity.ToolInputField;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ToolNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

@NodeHandlerType({NodeType.TOOL, NodeType.TOOL_LIB})
@Component
@RequiredArgsConstructor
public class ToolNodeHandler extends AbstractNodeHandler<ToolNode.NodeParams> {

    private final IOssService fileService;

    @Override
    protected Class<ToolNode.NodeParams> getParamsClass() {
        return ToolNode.NodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, ToolNode.NodeParams params) throws Exception {
        Map<String, Object> execParams = new HashMap<>(5);
        execParams.put("fileService", fileService);

        if (!CollectionUtils.isEmpty(params.getInputFieldList())) {
            for (ToolInputField inputField : params.getInputFieldList()) {
                Object value = workflow.getFieldValue(inputField.getValue(), inputField.getSource());
                execParams.put(inputField.getName(), value);
            }
        }

        GroovyScriptExecutor scriptExecutor = new GroovyScriptExecutor(params.getCode(), params.getInitParams());
        Object result = scriptExecutor.execute(execParams);

        // 使用辅助方法写入详情
        putDetail(node, "params", execParams);

        if (Boolean.TRUE.equals(params.getIsResult())) {
            setAnswer(node, result.toString());
        }

        return new NodeResult(Map.of("result", result));
    }
}
