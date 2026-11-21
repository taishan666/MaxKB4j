package com.maxkb4j.workflow.engine;

import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 断言分支路由的回归测试：
 * <ul>
 *   <li>BRANCH_ID 正常时按 "{sourceNodeId}_{branchId}_right" 锚点命中下游节点；</li>
 *   <li>BRANCH_ID 的 key 存在但值为 null（上游缺陷或恢复数据）时不得拼出 "null" 锚点，
 *   表现为不匹配任何边，而非错误路由（BUG-48）；</li>
 *   <li>BRANCH_ID 缺失时同样不匹配任何边。</li>
 * </ul>
 */
class WorkflowExecutionAccessorTest {

    @Test
    void findAssertionTargetNodeIds_matchesBranchAnchor() {
        LfEdge matched = edge("node-1", "node-1_branch-a_right", "node-2");
        LfEdge unmatched = edge("node-1", "node-1_branch-b_right", "node-3");

        Set<String> targets = WorkflowExecutionAccessor.findAssertionTargetNodeIds(
                new NodeResult(Map.of(NodeField.BRANCH_ID, "branch-a")), List.of(matched, unmatched));

        assertThat(targets).containsExactly("node-2");
    }

    @Test
    void findAssertionTargetNodeIds_treatsNullBranchIdValueAsNoMatch() {
        LfEdge edge = edge("node-1", "node-1_branch-a_right", "node-2");
        Map<String, Object> nodeVariable = new HashMap<>();
        nodeVariable.put(NodeField.BRANCH_ID, null);

        Set<String> targets = WorkflowExecutionAccessor.findAssertionTargetNodeIds(
                new NodeResult(nodeVariable), List.of(edge));

        assertThat(targets).isEmpty();
    }

    @Test
    void findAssertionTargetNodeIds_returnsEmptyWhenBranchIdMissing() {
        LfEdge edge = edge("node-1", "node-1_branch-a_right", "node-2");

        Set<String> targets = WorkflowExecutionAccessor.findAssertionTargetNodeIds(
                new NodeResult(Map.of()), List.of(edge));

        assertThat(targets).isEmpty();
    }

    private LfEdge edge(String sourceNodeId, String sourceAnchorId, String targetNodeId) {
        LfEdge edge = new LfEdge();
        edge.setSourceNodeId(sourceNodeId);
        edge.setSourceAnchorId(sourceAnchorId);
        edge.setTargetNodeId(targetNodeId);
        return edge;
    }
}
