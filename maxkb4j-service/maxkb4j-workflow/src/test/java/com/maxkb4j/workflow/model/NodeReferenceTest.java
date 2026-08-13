package com.maxkb4j.workflow.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NodeReference 解析契约测试（第 3 期引用类型化）。
 */
class NodeReferenceTest {

    @Test
    void parse_validList_returnsReference() {
        Optional<NodeReference> ref = NodeReference.parse(List.of("node-1", "answer"));

        assertThat(ref).contains(new NodeReference("node-1", "answer"));
    }

    @Test
    void parse_nullOrShortList_isEmpty() {
        assertThat(NodeReference.parse((List<String>) null)).isEmpty();
        assertThat(NodeReference.parse(List.of())).isEmpty();
        assertThat(NodeReference.parse(List.of("node-1"))).isEmpty();
    }

    @Test
    void parse_blankElements_isEmpty() {
        assertThat(NodeReference.parse(List.of("", "answer"))).isEmpty();
        assertThat(NodeReference.parse(List.of("node-1", "  "))).isEmpty();
    }

    @Test
    void parse_objectForm_validList() {
        Optional<NodeReference> ref = NodeReference.parse((Object) List.of("node-1", "answer"));

        assertThat(ref).contains(new NodeReference("node-1", "answer"));
    }

    @Test
    void parse_objectForm_nonStringOrNonList_isEmpty() {
        assertThat(NodeReference.parse((Object) List.of("node-1", 42))).isEmpty();
        assertThat(NodeReference.parse((Object) "node-1")).isEmpty();
        assertThat(NodeReference.parse((Object) null)).isEmpty();
    }
}