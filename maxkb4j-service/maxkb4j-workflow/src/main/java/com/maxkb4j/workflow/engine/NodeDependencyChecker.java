package com.maxkb4j.workflow.engine;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 节点依赖检查器
 * <p>
 * 负责基于上下游边与节点状态判断节点执行前置条件：
 * <ul>
 *   <li>依赖节点是否已全部执行完成（{@link #dependenciesNotExecuted}）</li>
 *   <li>节点是否可跳过（上游节点全部为 SKIP，{@link #isSkipNode}）</li>
 * </ul>
 * </p>
 */
public class NodeDependencyChecker {

    /**
     * 工作流配置（用于按 ID 查询节点状态）
     */
    private final WorkflowConfiguration configuration;
    /**
     * 边导航器（用于查询上游节点 ID）
     */
    private final EdgeNavigator navigator;

    public NodeDependencyChecker(WorkflowConfiguration configuration, EdgeNavigator navigator) {
        this.configuration = configuration;
        this.navigator = navigator;
    }

    /**
     * 检查依赖节点是否已执行
     *
     * @param node 待检查节点
     * @return 是否所有依赖节点都已执行
     */
    public boolean dependenciesNotExecuted(AbsNode node) {
        List<String> upNodeIdList = navigator.findUpstreamNodeIds(node.getId());
        // 开始节点无上游依赖，直接通过
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return false;
        }
        // 多个上游节点时，检查是否所有上游节点都是 SKIP（排除这种情况）
        List<AbsNode> upNodes = configuration.getNodes().stream()
                .filter(n -> upNodeIdList.contains(n.getId()))
                .toList();
        return !upNodes.stream()
                .allMatch(n -> NodeStatus.SUCCESS.getStatus() == n.getStatus()
                        || NodeStatus.SKIP.getStatus() == n.getStatus());
    }

    /**
     * 检查是否为可跳过节点
     * 所有上游节点均为 SKIP 时视为可跳过
     *
     * @param node 待检查节点
     * @return 是否可跳过
     */
    public boolean isSkipNode(AbsNode node) {
        List<String> upNodeIdList = navigator.findUpstreamNodeIds(node.getId());
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return true;
        }
        // 多个上游节点时，检查是否所有上游节点都是 SKIP（排除这种情况）
        List<AbsNode> upNodes = configuration.getNodes().stream()
                .filter(n -> upNodeIdList.contains(n.getId()))
                .toList();
        return upNodes.stream().allMatch(n -> NodeStatus.SKIP.getStatus() == n.getStatus());
    }

    /**
     * 检查是否为可跳过节点（排除指定上游节点后，剩余上游全部为 SKIP）
     *
     * @param node          待检查节点
     * @param excludeNodeId 需要排除的上游节点 ID（通常是当前分支节点自身）
     * @return 是否可跳过
     */
    public boolean isAssertionSkipNode(AbsNode node, String excludeNodeId) {
        List<String> upNodeIdList = navigator.findUpstreamNodeIds(node.getId());
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return true;
        }
        // 排除当前节点后剩余的上游节点集合
        Set<String> upNodeIdSet = upNodeIdList.stream()
                .filter(id -> !id.equals(excludeNodeId))
                .collect(Collectors.toSet());
        // 排除后无其他上游依赖，视为可跳过
        if (upNodeIdSet.isEmpty()) {
            return true;
        }
        // 检查剩余上游节点是否全部处于 SKIP 状态
        List<AbsNode> upNodes = configuration.getNodes().stream()
                .filter(n -> upNodeIdSet.contains(n.getId()))
                .toList();

        List<AbsNode> upNotSkipNodes = upNodes.stream().filter(n->NodeStatus.SKIP.getStatus() != n.getStatus())
                .toList();
        if (upNotSkipNodes.isEmpty()) {
            return true;
        }
        // todo
        return upNodes.stream().allMatch(n -> (NodeStatus.SKIP.getStatus() == n.getStatus()||NodeStatus.SUCCESS.getStatus() == n.getStatus()));
    }
}
