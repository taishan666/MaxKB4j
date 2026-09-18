package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.dto.ToolInputField;
import com.maxkb4j.tool.service.IToolExecuteService;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.impl.ToolNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.VariableField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：工具节点输入字段引用上游节点输出时（配置中 value 为 [nodeId, field] 数组），
 * 必须先解析为真实值再交给工具执行，否则脚本拿到的是 fastjson2 JSONArray 而非字符串
 * （如 text.startsWith('【') 抛 MissingMethodException）。
 */
class ToolNodeHandlerTest {

    private final AtomicReference<Map<String, Object>> executedParams = new AtomicReference<>();

    private ToolNodeHandler newHandler() throws IOException {
        IToolExecuteService service = mock(IToolExecuteService.class);
        when(service.convertParamType(anyList())).thenAnswer(inv -> {
            List<ToolInputField> fields = inv.getArgument(0);
            Map<String, Object> params = new HashMap<>();
            if (fields != null) {
                fields.forEach(f -> params.put(f.getName(), f.getValue()));
            }
            return params;
        });
        when(service.httpOrCodeExecute(anyString(), anyString(), any(), anyMap()))
                .thenAnswer(inv -> {
                    executedParams.set(inv.getArgument(3));
                    return "ok";
                });
        return new ToolNodeHandler(service);
    }

    private ToolNode newNode(Object inputValue, String source) {
        JSONObject inputField = new JSONObject();
        inputField.put("name", "text");
        inputField.put("type", "string");
        inputField.put("value", inputValue);
        inputField.put("source", source);

        JSONObject nodeData = new JSONObject();
        nodeData.put("toolType", "CUSTOM");
        nodeData.put("code", "return text");
        nodeData.put("inputFieldList", List.of(inputField));

        JSONObject properties = new JSONObject();
        properties.put(RuntimeDetailField.NODE_DATA, nodeData);
        return new ToolNode("tool1", properties);
    }

    private IWorkflow workflowReturning(Object upstreamValue) {
        IWorkflow workflow = mock(IWorkflow.class);
        when(workflow.getFieldValue(any(), eq(VariableField.REFERENCE))).thenReturn(upstreamValue);
        return workflow;
    }

    @Test
    void referenceValue_resolvedBeforeExecution() throws Exception {
        // 节点配置中的引用：value 为 ["aiNode", "answer"] 数组，上游输出为字符串
        ToolNode node = newNode(List.of("aiNode", "answer"), VariableField.REFERENCE);
        IWorkflow workflow = workflowReturning("【你好】");

        NodeResult result = newHandler().execute(workflow, node).get();

        // 工具拿到的是解析后的字符串，而非 fastjson2 JSONArray
        assertThat(executedParams.get().get("text")).isEqualTo("【你好】");
        Map<String, Object> params = (Map<String, Object>) node.getDetail().get(NodeField.PARAMS);
        assertThat(params).containsEntry("text", "【你好】");
        assertThat(result.getNodeVariable()).containsEntry(NodeField.RESULT, "ok");
    }

    @Test
    void referenceShapedValue_resolvedEvenWithoutSourceFlag() throws Exception {
        // 兼容未显式携带 source=reference 的历史配置：value 形如 [nodeId, field] 同样按引用解析
        ToolNode node = newNode(List.of("aiNode", "answer"), null);
        IWorkflow workflow = workflowReturning("【你好】");

        newHandler().execute(workflow, node).get();

        assertThat(executedParams.get().get("text")).isEqualTo("【你好】");
    }

    @Test
    void literalValue_passedThroughUnchanged() throws Exception {
        ToolNode node = newNode("【你好】", "custom");
        IWorkflow workflow = workflowReturning("不应被引用解析覆盖");

        newHandler().execute(workflow, node).get();

        assertThat(executedParams.get().get("text"))
                .isEqualTo("【你好】")
                .isInstanceOf(String.class);
    }
}
