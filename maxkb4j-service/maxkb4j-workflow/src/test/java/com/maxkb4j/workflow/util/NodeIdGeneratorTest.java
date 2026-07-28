package com.maxkb4j.workflow.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：运行时节点 ID 生成流程（基于 SHA-1，输入决定输出）。
 */
class NodeIdGeneratorTest {

    @Test
    void isDeterministicForSameInput() {
        assertThat(NodeIdGenerator.generateRuntimeNodeId("n1", null))
                .isEqualTo(NodeIdGenerator.generateRuntimeNodeId("n1", null));
    }

    @Test
    void nullUpListEqualsEmptyUpList() {
        assertThat(NodeIdGenerator.generateRuntimeNodeId("n1", null))
                .isEqualTo(NodeIdGenerator.generateRuntimeNodeId("n1", List.of()));
    }

    @Test
    void differsWhenUpNodeListChanges() {
        assertThat(NodeIdGenerator.generateRuntimeNodeId("n1", List.of("a")))
                .isNotEqualTo(NodeIdGenerator.generateRuntimeNodeId("n1", List.of("b")));
    }

    @Test
    void differsWhenNodeIdChanges() {
        assertThat(NodeIdGenerator.generateRuntimeNodeId("n1", List.of("a")))
                .isNotEqualTo(NodeIdGenerator.generateRuntimeNodeId("n2", List.of("a")));
    }

    @Test
    void producesLowercaseHexOfSha1Length() {
        String id = NodeIdGenerator.generateRuntimeNodeId("n1", List.of("a"));
        assertThat(id).hasSize(40);
        assertThat(id).matches("[0-9a-f]{40}");
    }
}