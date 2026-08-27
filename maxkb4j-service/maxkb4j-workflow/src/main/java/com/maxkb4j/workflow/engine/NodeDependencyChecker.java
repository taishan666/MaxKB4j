package com.maxkb4j.workflow.engine;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.List;

/**
 * 节点依赖检查器
 * <p>
 * 负责基于上下游边与节点状态判断节点执行前置条件：
 * <ul>
 *   <li>依赖节点是否已全部执行完成（{@link #dependenciesNotExecuted}）</li>
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
        List<AbsNode> upNodes = configuration.getNodes().stream()
                .filter(n -> upNodeIdList.contains(n.getId()))
                .toList();
        return !upNodes.stream()
                .allMatch(n -> (NodeStatus.SUCCESS.getStatus() == n.getStatus()||NodeStatus.SKIP.getStatus() == n.getStatus()));
    }
}
