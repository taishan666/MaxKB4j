package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试:工作流配置管理器(不可变拷贝、节点映射去重、按 ID 查找、空安全)。
 */
class WorkflowConfigurationTest {

    private AbsNode newNode(String id) {
        return new AbsNode(id, new JSONObject()) {};
    }

    @Test
    void constructor_nullNodesAndEdges_yieldsEmptyImmutableCollections() {
        WorkflowConfiguration configuration = new WorkflowConfiguration(WorkflowMode.APPLICATION, null, null);

        assertThat(configuration.getNodes()).isEmpty();
        assertThat(configuration.getEdges()).isEmpty();
        assertThat(configuration.getNode("any")).isNull();
    }

    @Test
    void nodesAndEdgesAreUnmodifiable() {
        AbsNode node = newNode("n1");
        WorkflowConfiguration configuration = new WorkflowConfiguration(
                WorkflowMode.APPLICATION, List.of(node), List.of());

        assertThatThrownBy(() -> configuration.getNodes().add(newNode("n2")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> configuration.getEdges().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getNode_returnsByNodeIdOrNull() {
        AbsNode n1 = newNode("n1");
        AbsNode n2 = newNode("n2");
        WorkflowConfiguration configuration = new WorkflowConfiguration(
                WorkflowMode.APPLICATION, List.of(n1, n2), List.of());

        assertThat(configuration.getNode("n1")).isSameAs(n1);
        assertThat(configuration.getNode("n2")).isSameAs(n2);
        assertThat(configuration.getNode("missing")).isNull();
    }

    @Test
    void nodeMap_deduplicatesByKeepingFirstOnDuplicateIds() {
        AbsNode first = newNode("dup");
        AbsNode second = newNode("dup");
        WorkflowConfiguration configuration = new WorkflowConfiguration(
                WorkflowMode.APPLICATION, List.of(first, second), List.of());

        // 相同 ID 取首个,后续重复项被丢弃
        assertThat(configuration.getNode("dup")).isSameAs(first);
        assertThat(configuration.getNodes()).hasSize(2);
    }

    @Test
    void chatParamsSetterAndGettersRoundTrip() {
        WorkflowConfiguration configuration = new WorkflowConfiguration(
                WorkflowMode.KNOWLEDGE_LOOP, List.of(), List.of());
        assertThat(configuration.getWorkflowMode()).isEqualTo(WorkflowMode.KNOWLEDGE_LOOP);
        assertThat(configuration.getChatParams()).isNull();

        ChatParams params = ChatParams.builder().chatRecordId("rec-1").build();
        configuration.setChatParams(params);

        assertThat(configuration.getChatParams()).isSameAs(params);
        assertThat(configuration.getChatParams().getChatRecordId()).isEqualTo("rec-1");
    }
}