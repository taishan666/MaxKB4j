package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：节点依赖检查器（依赖就绪判定与 SKIP 传导判定）。
 */
class NodeDependencyCheckerTest {

    private AbsNode node(String id) {
        return new AbsNode(id, new JSONObject()) {};
    }

    private LfEdge edge(String source, String target) {
        LfEdge edge = new LfEdge();
        edge.setSourceNodeId(source);
        edge.setTargetNodeId(target);
        return edge;
    }

    private NodeDependencyChecker checker(List<AbsNode> nodes, List<LfEdge> edges) {
        WorkflowConfiguration configuration = new WorkflowConfiguration(WorkflowMode.KNOWLEDGE, nodes, edges);
        return new NodeDependencyChecker(configuration, new EdgeNavigator(edges));
    }

    private AbsNode withStatus(AbsNode node, NodeStatus status) {
        node.setStatus(status.getStatus());
        return node;
    }

    @Test
    void dependenciesNotExecuted_returnsTrueWhenAnyUpstreamPending() {
        AbsNode a = withStatus(node("a"), NodeStatus.SUCCESS);
        AbsNode b = withStatus(node("b"), NodeStatus.READY);
        AbsNode c = node("c");
        NodeDependencyChecker checker = checker(List.of(a, b, c), List.of(edge("a", "c"), edge("b", "c")));

        assertThat(checker.dependenciesNotExecuted(c)).isTrue();
    }

    @Test
    void dependenciesNotExecuted_returnsFalseWhenAllUpstreamFinishedOrSkipped() {
        AbsNode a = withStatus(node("a"), NodeStatus.SUCCESS);
        AbsNode b = withStatus(node("b"), NodeStatus.SKIP);
        AbsNode c = node("c");
        NodeDependencyChecker checker = checker(List.of(a, b, c), List.of(edge("a", "c"), edge("b", "c")));

        assertThat(checker.dependenciesNotExecuted(c)).isFalse();
    }

    @Test
    void dependenciesNotExecuted_startNodeWithoutUpstream_returnsFalse() {
        AbsNode a = node("a");
        AbsNode b = node("b");
        NodeDependencyChecker checker = checker(List.of(a, b), List.of(edge("a", "b")));

        assertThat(checker.dependenciesNotExecuted(a)).isFalse();
    }

}
