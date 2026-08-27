package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：节点状态加载器（节点实例化与执行状态恢复）。
 */
class NodeStateLoaderTest {

    private AbsNode node(String id) {
        return new AbsNode(id, new JSONObject()) {};
    }

    private AbsNode nodeWithNodeData(String id) {
        JSONObject nodeData = new JSONObject();
        nodeData.put("config", "value");
        JSONObject properties = new JSONObject();
        properties.put(RuntimeDetailField.NODE_DATA, nodeData);
        return new AbsNode(id, properties) {};
    }

    private Map<String, Object> detail(int index, String nodeId, List<String> upNodeIds,
                                       String runtimeNodeId, Integer status) {
        Map<String, Object> detail = new HashMap<>();
        detail.put(RuntimeDetailField.INDEX, index);
        detail.put(RuntimeDetailField.NODE_ID, nodeId);
        detail.put(RuntimeDetailField.UP_NODE_ID_LIST, upNodeIds);
        detail.put(RuntimeDetailField.RUNTIME_NODE_ID, runtimeNodeId);
        detail.put(RuntimeDetailField.STATUS, status);
        return detail;
    }

    private NodeStateLoader loader(List<AbsNode> nodes, WorkflowContext context) {
        WorkflowConfiguration configuration = new WorkflowConfiguration(WorkflowMode.KNOWLEDGE, nodes, List.of());
        return new NodeStateLoader(configuration, context);
    }

    @Test
    void getNodeInstance_wiresUpstreamAndAppliesPropertiesMapper() {
        AbsNode n1 = node("n1");
        NodeStateLoader stateLoader = loader(List.of(n1), new WorkflowContext());
        AtomicBoolean mapperApplied = new AtomicBoolean(false);

        AbsNode result = stateLoader.getNodeInstance("n1", List.of("up1"), n -> {
            mapperApplied.set(true);
            return n.getProperties();
        });

        assertThat(result).isSameAs(n1);
        assertThat(result.getUpNodeIdList()).containsExactly("up1");
        assertThat(mapperApplied).isTrue();
    }

    @Test
    void getNodeInstance_unknownNodeReturnsNull() {
        NodeStateLoader stateLoader = loader(List.of(node("n1")), new WorkflowContext());

        assertThat(stateLoader.getNodeInstance("nope", List.of(), null)).isNull();
    }

    @Test
    void loadNodeState_restoresNodesIntoContext_andReturnsCurrentNode() {
        AbsNode n1 = node("n1");
        AbsNode n2 = nodeWithNodeData("n2");
        WorkflowContext context = new WorkflowContext();
        NodeStateLoader stateLoader = loader(List.of(n1, n2), context);

        JSONObject details = new JSONObject();
        details.put("d1", detail(0, "n1", List.of(), "rt-1", NodeStatus.SUCCESS.getStatus()));
        details.put("d2", detail(1, "n2", List.of("n1"), "rt-2", NodeStatus.SUCCESS.getStatus()));

        Map<String, Object> currentNodeData = new HashMap<>();
        currentNodeData.put("answer", "hello");
        AbsNode restored = stateLoader.loadNodeState(null, details, "rt-2", currentNodeData);

        assertThat(restored).isSameAs(n2);
        assertThat(restored.getStatus()).isEqualTo(NodeStatus.SUCCESS.getStatus());
        assertThat(restored.getProperties().getJSONObject(RuntimeDetailField.NODE_DATA))
                .containsEntry("form_data", currentNodeData);
        // 按 index 顺序恢复全部节点到上下文
        assertThat(context.getNodeContext()).containsExactly(n1, n2);
    }

    @Test
    void loadNodeState_currentNodeNotFound_returnsNullButRestoresOthers() {
        AbsNode n1 = node("n1");
        WorkflowContext context = new WorkflowContext();
        NodeStateLoader stateLoader = loader(List.of(n1), context);

        JSONObject details = new JSONObject();
        details.put("d1", detail(0, "n1", List.of(), "rt-1", NodeStatus.SUCCESS.getStatus()));

        AbsNode restored = stateLoader.loadNodeState(null, details, "not-found", null);

        assertThat(restored).isNull();
        assertThat(context.getNodeContext()).containsExactly(n1);
    }

    @Test
    void loadNodeState_nullDetailsOrCurrentNodeId_returnsNull() {
        AbsNode n1 = node("n1");
        NodeStateLoader stateLoader = loader(List.of(n1), new WorkflowContext());

        assertThat(stateLoader.loadNodeState(null, null, "rt-1", null)).isNull();
        assertThat(stateLoader.loadNodeState(null, new JSONObject(), null, null)).isNull();
    }

    @Test
    void loadNodeState_injectsFormDataOnlyIntoCurrentNode() {
        AbsNode n1 = node("n1");
        AbsNode n2 = nodeWithNodeData("n2");
        WorkflowContext context = new WorkflowContext();
        NodeStateLoader stateLoader = loader(List.of(n1, n2), context);

        JSONObject details = new JSONObject();
        details.put("d1", detail(0, "n1", List.of(), "rt-1", NodeStatus.SUCCESS.getStatus()));
        details.put("d2", detail(1, "n2", List.of("n1"), "rt-2", NodeStatus.SUCCESS.getStatus()));

        Map<String, Object> currentNodeData = new HashMap<>();
        currentNodeData.put("answer", "hello");
        stateLoader.loadNodeState(null, details, "rt-2", currentNodeData);

        // 其他节点不注入 form_data
        assertThat(n1.getProperties().getJSONObject(RuntimeDetailField.NODE_DATA)).isNull();
        assertThat(n2.getProperties().getJSONObject(RuntimeDetailField.NODE_DATA))
                .containsEntry("form_data", currentNodeData);
    }
}
