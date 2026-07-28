package com.maxkb4j.workflow.engine;

import com.maxkb4j.workflow.logic.LfEdge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：工作流边导航流程（上下游查找、空安全、计数）。
 */
class EdgeNavigatorTest {

    private LfEdge edge(String source, String target) {
        LfEdge e = new LfEdge();
        e.setSourceNodeId(source);
        e.setTargetNodeId(target);
        return e;
    }

    private EdgeNavigator nav() {
        return new EdgeNavigator(List.of(
                edge("A", "B"),
                edge("A", "C"),
                edge("B", "D")));
    }

    @Test
    void findDownstreamEdges_returnsEdgesFromNode() {
        EdgeNavigator navigator = nav();
        assertThat(navigator.findDownstreamEdges("A")).hasSize(2);
        assertThat(navigator.findDownstreamEdges("B")).hasSize(1);
        assertThat(navigator.findDownstreamEdges("nope")).isEmpty();
        assertThat(navigator.findDownstreamEdges(null)).isEmpty();
    }

    @Test
    void findUpstreamNodeIds_returnsSourceNodes() {
        EdgeNavigator navigator = nav();
        assertThat(navigator.findUpstreamNodeIds("D")).containsExactly("B");
        assertThat(navigator.findUpstreamNodeIds("B")).containsExactly("A");
        assertThat(navigator.findUpstreamNodeIds("A")).isEmpty();
        assertThat(navigator.findUpstreamNodeIds(null)).isEmpty();
    }

    @Test
    void nullEdgesYieldsEmptyNavigator() {
        EdgeNavigator navigator = new EdgeNavigator(null);
        assertThat(navigator.isEmpty()).isTrue();
        assertThat(navigator.size()).isZero();
        assertThat(navigator.findDownstreamEdges("A")).isEmpty();
    }

    @Test
    void sizeAndEmptyReflectEdgeCount() {
        EdgeNavigator navigator = nav();
        assertThat(navigator.isEmpty()).isFalse();
        assertThat(navigator.size()).isEqualTo(3);
    }
}