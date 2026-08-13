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
 *
 * <p>新语义（commit 653cf2f3b）：起始节点 = type 以 "data-source-" 开头的节点；
 * dataSource 或其 nodeId 为 null 时返回空列表且不标记任何节点。</p>
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
    void startNodes_returnsDataSourceNodes_andMarksNonSelectedAsSkip() {
        AbsNode selectedNode = node("ds1", NodeType.DATA_SOURCE_LOCAL.getKey());
        AbsNode otherDataSourceNode = node("ds2", NodeType.DATA_SOURCE_WEB.getKey());
        AbsNode downstreamNode = node("n1", NodeType.BASE.getKey());
        KnowledgeWorkflow workflow = workflow(
                List.of(selectedNode, otherDataSourceNode, downstreamNode),
                List.of(edge("ds1", "n1")),
                dataSourceOf("ds1"));

        List<AbsNode> startNodes = workflow.startNodes();

        assertThat(startNodes).containsExactly(selectedNode, otherDataSourceNode);
        assertThat(selectedNode.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
        assertThat(otherDataSourceNode.getStatus()).isEqualTo(NodeStatus.SKIP.getStatus());
        assertThat(downstreamNode.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
    }

    @Test
    void startNodes_excludesNonDataSourceNodes() {
        AbsNode knowledgeBaseNode = node("kb1", NodeType.KNOWLEDGE_BASE.getKey());
        AbsNode nullTypeNode = new AbsNode("nt1", new JSONObject()) {};
        AbsNode dataSourceNode = node("ds1", NodeType.DATA_SOURCE_LOCAL.getKey());
        KnowledgeWorkflow workflow = workflow(
                List.of(knowledgeBaseNode, nullTypeNode, dataSourceNode),
                List.of(),
                dataSourceOf("ds1"));

        assertThat(workflow.startNodes()).containsExactly(dataSourceNode);
        assertThat(knowledgeBaseNode.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
    }

    @Test
    void startNodes_withNullDataSource_returnsEmptyAndMarksNothing() {
        // 回归：dataSource 来自请求体，旧实现对 null 直接解引用导致 NPE
        AbsNode dataSourceNode = node("ds1", NodeType.DATA_SOURCE_LOCAL.getKey());
        KnowledgeWorkflow workflow = workflow(List.of(dataSourceNode), List.of(), null);

        assertThatCode(workflow::startNodes).doesNotThrowAnyException();
        assertThat(workflow.startNodes()).isEmpty();
        assertThat(dataSourceNode.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
    }

    @Test
    void startNodes_withNullDataSourceNodeId_returnsEmptyAndMarksNothing() {
        AbsNode dataSourceNode = node("ds1", NodeType.DATA_SOURCE_LOCAL.getKey());
        KnowledgeWorkflow workflow = workflow(List.of(dataSourceNode), List.of(), dataSourceOf(null));

        assertThat(workflow.startNodes()).isEmpty();
        assertThat(dataSourceNode.getStatus()).isEqualTo(NodeStatus.READY.getStatus());
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