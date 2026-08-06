package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.node.AbsNode;

import java.util.List;

/**
 * 工作流执行控制器接口（契约层）。
 * <p>
 * 定义节点状态恢复、下一节点计算、依赖检查、执行追踪等执行控制行为，供 {@link IWorkflow} 门面返回。
 * 具体实现位于 workflow 实现模块（其中节点状态加载、节点实例获取等内部方法不在本接口暴露）。
 */
public interface IWorkflowExecutionAccessor {

    /**
     * 获取当前执行节点。
     */
    AbsNode currentNode();

    /**
     * 获取开始节点。
     */
    AbsNode startNode();

    /**
     * 获取下一节点列表（根据当前节点执行结果与下游边计算，处理断言分支与跳过）。
     */
    List<AbsNode> nextNodes(AbsNode currentNode, NodeResult currentNodeResult);

    /**
     * 检查依赖节点是否已执行（开始节点无上游依赖直接通过）。
     */
    boolean dependenciesExecuted(AbsNode node);

    /**
     * 检查是否为可跳过节点（所有上游节点均为 SKIP）。
     */
    boolean isSkipNode(AbsNode node);

    /**
     * 检查是否为可跳过节点（排除指定上游节点后，剩余上游均为 SKIP）。
     */
    boolean isSkipNode(AbsNode node, String excludeNodeId);

    /**
     * 记录节点执行（runtimeNodeId 顺序与时间戳）。
     */
    void recordExecution(AbsNode node);
}
