package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.node.AbsNode;

import java.util.List;

/**
 * 执行访问器
 * 提供执行控制相关方法，封装 WorkflowExecutionController 的直接访问
 *
 * 设计原则：
 * - 提供清晰的语义化方法名
 * - 封装执行状态管理逻辑
 * - 统一执行控制入口
 */
public class ExecutionAccessor {

    private final WorkflowExecutionController executor;

    ExecutionAccessor(WorkflowExecutionController executor) {
        this.executor = executor;
    }

    // ==================== 状态读取 ====================

    /**
     * 获取当前执行节点
     *
     * @return 当前节点实例
     */
    public AbsNode currentNode() {
        return executor.getCurrentNode();
    }

    /**
     * 获取开始节点
     *
     * @return 开始节点实例
     */
    public AbsNode startNode() {
        return executor.getStartNode();
    }

    /**
     * 获取下一节点列表
     *
     * @param current 当前节点
     * @param result  当前节点执行结果
     * @return 下一节点列表
     */
    public List<AbsNode> nextNodes(AbsNode current, NodeResult result) {
        return executor.getNextNodeList(current, result);
    }

    // ==================== 状态写入 ====================

    /**
     * 设置当前执行节点
     *
     * @param node 节点实例
     */
    public void setCurrentNode(AbsNode node) {
        executor.setCurrentNode(node);
    }

    // ==================== 检查方法 ====================

    /**
     * 检查依赖节点是否已执行
     *
     * @param node 待检查节点
     * @return 是否所有依赖节点都已执行
     */
    public boolean dependenciesExecuted(AbsNode node) {
        return executor.dependentNodeBeenExecuted(node);
    }

    /**
     * 检查是否为就绪的 Join 节点
     * Join 节点需要等待所有上游节点执行完成
     *
     * @param node 待检查节点
     * @return 是否为就绪的 Join 节点
     */
    public boolean isReadyJoin(AbsNode node) {
        return executor.isReadyJoinNode(node);
    }

    /**
     * 获取节点实例
     *
     * @param nodeId 节点 ID
     * @return 节点实例
     */
    public AbsNode getNode(String nodeId) {
        return executor.getConfiguration().getNode(nodeId);
    }

    /**
     * 检查节点是否存在
     *
     * @param nodeId 节点 ID
     * @return 是否存在
     */
    public boolean hasNode(String nodeId) {
        return executor.getConfiguration().hasNode(nodeId);
    }

    /**
     * 获取节点总数
     *
     * @return 节点数量
     */
    public int nodeCount() {
        return executor.getConfiguration().nodeCount();
    }

    /**
     * 获取边总数
     *
     * @return 边数量
     */
    public int edgeCount() {
        return executor.getConfiguration().edgeCount();
    }
}