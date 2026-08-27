package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：执行追踪器（执行路径与时间戳记录、空安全）。
 */
class ExecutionTrackerTest {

    private AbsNode newNode(String id) {
        return new AbsNode(id, new JSONObject()) {};
    }

    @Test
    void recordExecution_appendsPathAndTimestampInOrder() {
        ExecutionTracker tracker = new ExecutionTracker();
        AbsNode node1 = newNode("n1");
        AbsNode node2 = newNode("n2");

        tracker.recordExecution(node1);
        tracker.recordExecution(node2);

        assertThat(tracker.getExecutionPath())
                .containsExactly(node1.getRuntimeNodeId(), node2.getRuntimeNodeId());
        assertThat(tracker.getExecutionTimestamps())
                .containsOnlyKeys(node1.getRuntimeNodeId(), node2.getRuntimeNodeId());
    }

    @Test
    void recordExecution_ignoresNullNodeOrMissingRuntimeNodeId() {
        ExecutionTracker tracker = new ExecutionTracker();
        tracker.recordExecution(null);

        AbsNode node = newNode("n1");
        node.setRuntimeNodeId(null);
        tracker.recordExecution(node);

        assertThat(tracker.getExecutionPath()).isEmpty();
        assertThat(tracker.getExecutionTimestamps()).isEmpty();
    }

    @Test
    void recordExecution_sameRuntimeNodeIdRecordsEachExecutionInPath() {
        ExecutionTracker tracker = new ExecutionTracker();
        AbsNode node = newNode("n1");
        String runtimeNodeId = node.getRuntimeNodeId();

        // 原语义：路径记录每次执行（可重复），时间戳保留最近一次
        tracker.recordExecution(node);
        tracker.recordExecution(node);

        assertThat(tracker.getExecutionPath()).containsExactly(runtimeNodeId, runtimeNodeId);
        assertThat(tracker.getExecutionTimestamps()).containsOnlyKeys(runtimeNodeId);
    }
}