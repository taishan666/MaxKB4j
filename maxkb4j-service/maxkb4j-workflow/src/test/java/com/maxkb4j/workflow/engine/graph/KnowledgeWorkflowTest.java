package com.maxkb4j.workflow.engine.graph;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.DataSource;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 回归测试：KnowledgeWorkflow 起始节点计算与空安全。
 */
class KnowledgeWorkflowTest {

    private AbsNode node(String id, String type) {
        AbsNode node = new AbsNode(id, new JSONObject()) {};
        node.setType(type);
        return node;
    }

    private LfEdge edge(String source, String target) {
        LfEdge edge = new LfEdge();
        edge.setSourceNodeId(source);
        edge.setTargetNodeId(target);
        return edge;
    }

    private KnowledgeWorkflow workflow(List<AbsNode> nodes, List<LfEdge> edges, DataSource dataSource) {
        KnowledgeParams params = KnowledgeParams.builder().dataSource(dataSource).build();
        return new KnowledgeWorkflow(nodes, edges, params);
    }

    @Test
    void getStartNodes_marksNonDataSourceStartNodesAsSkip() {
        AbsNode dataSourceNode = node("ds1", "base-node");
        AbsNode otherStartNode = node("ds2", "base-node");
        AbsNode downstreamNode = node("n1", "base-node");
        KnowledgeWorkflow workflow = workflow(
                List.of(dataSourceNode, otherStartNode, downstreamNode),
                List.of(edge("ds1", "n1")),
                dataSourceOf("ds1"));

        List<AbsNode> startNodes = workflow.getStartNodes();

        assertThat(startNodes).containsExactly(dataSourceNode, otherStartNode);
        assertThat(dataSourceNode.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
        assertThat(otherStartNode.getStatus()).isEqualTo(NodeStatus.SKIP.getStatus());
    }

    @Test
    void getStartNodes_excludesKnowledgeBaseNodes() {
        AbsNode knowledgeBaseNode = node("kb1", NodeType.KNOWLEDGE_BASE.getKey());
        AbsNode startNode = node("ds1", "base-node");
        KnowledgeWorkflow workflow = workflow(
                List.of(knowledgeBaseNode, startNode),
                List.of(),
                dataSourceOf("ds1"));

        assertThat(workflow.getStartNodes()).containsExactly(startNode);
    }

    @Test
    void getStartNodes_withNullDataSource_doesNotThrowAndSkipsNothing() {
        // 回归：dataSource 来自请求体，旧实现对 null 直接解引用导致 NPE
        AbsNode startNode = node("ds1", "base-node");
        KnowledgeWorkflow workflow = workflow(List.of(startNode), List.of(), null);

        assertThatCode(workflow::getStartNodes).doesNotThrowAnyException();
        assertThat(workflow.getStartNodes()).containsExactly(startNode);
        assertThat(startNode.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
    }

    @Test
    void getStartNodes_withNullDataSourceNodeId_skipsMarking() {
        AbsNode startNode = node("ds1", "base-node");
        KnowledgeWorkflow workflow = workflow(List.of(startNode), List.of(), dataSourceOf(null));

        assertThat(workflow.getStartNodes()).containsExactly(startNode);
        assertThat(startNode.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
    }

    @Test
    void constructor_injectsKnowledgeBaseIntoGlobalContext() {
        KnowledgeParams params = KnowledgeParams.builder()
                .knowledgeBase(Map.of("docId", "d1"))
                .build();
        KnowledgeWorkflow workflow = new KnowledgeWorkflow(List.of(), List.of(), params);

        assertThat(workflow.getGlobalContext()).containsEntry("docId", "d1");
    }

    private DataSource dataSourceOf(String nodeId) {
        DataSource dataSource = new DataSource();
        dataSource.setNodeId(nodeId);
        return dataSource;
    }
}
