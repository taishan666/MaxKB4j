package com.maxkb4j.workflow.handler.node.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

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
        Map<String, Object> execParams = new HashMap<>(5);
        if (!CollectionUtils.isEmpty(params.getInputFieldList())) {
            for (ToolInputField inputField : params.getInputFieldList()) {
                Object value = workflow.getFieldValue(inputField.getValue(), inputField.getSource());
                execParams.put(inputField.getName(), convertValue(inputField.getType(), value));
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
        putDetail(node, NodeField.PARAMS, execParams);
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

    /**
     * 将调试字段的字符串 value 按 dataType 转换为对应类型
     *
     * @param dataType 数据类型：string、int、dict、array、float、boolean
     * @param value    原始值（字符串）
     * @return 转换后的值，转换失败时返回原始值
     */
    private Object convertValue(String dataType, Object value) {
        if (!(value instanceof String str) || StringUtils.isBlank(str) || StringUtils.isBlank(dataType)) {
            return value;
        }
        String trimmed = str.trim();
        try {
            return switch (dataType.toLowerCase()) {
                case "int" -> Long.parseLong(trimmed);
                case "float" -> Double.parseDouble(trimmed);
                case "boolean" -> Boolean.parseBoolean(trimmed);
                case "dict" -> JSONUtil.isTypeJSONObject(trimmed) ? JSONUtil.parseObj(trimmed) : value;
                case "array" -> JSONUtil.isTypeJSONArray(trimmed) ? JSONUtil.parseArray(trimmed) : value;
                default -> str;
            };
        } catch (NumberFormatException e) {
            log.warn("Failed to convert debug field value [{}] to type [{}]", str, dataType);
            return value;
        }
    }
}
