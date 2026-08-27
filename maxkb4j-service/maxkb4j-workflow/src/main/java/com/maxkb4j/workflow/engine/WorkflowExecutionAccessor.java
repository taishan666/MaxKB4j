package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.IWorkflowExecutionAccessor;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;

/**
 * 工作流执行控制器（组合门面）
 * <p>
 * 负责编排执行控制各子组件，对外屏蔽内部协作细节：
 * <ul>
 *   <li>{@link EdgeNavigator} — 上下游边查找</li>
 *   <li>{@link NodeDependencyChecker} — 依赖就绪与 SKIP 判定</li>
 *   <li>{@link NodeStateLoader} — 节点实例化与执行状态恢复</li>
 *   <li>{@link ExecutionTracker} — 执行路径与时间戳记录</li>
 * </ul>
 * 本类自身仅保留下一节点计算（含断言分支处理）。
 * </p>
 */
public class WorkflowExecutionAccessor implements IWorkflowExecutionAccessor {

    /**
     * 边导航器
     */
    private final EdgeNavigator navigator;
    /**
     * 节点依赖检查器
     */
    private final NodeDependencyChecker dependencyChecker;
    /**
     * 节点状态加载器
     */
    private final NodeStateLoader stateLoader;
    /**
     * 执行追踪器
     */
    private final ExecutionTracker executionTracker;
    /**
     * 当前执行节点
     */
    private AbsNode currentNode;

    public WorkflowExecutionAccessor(WorkflowConfiguration configuration,
                                     WorkflowContext context,
                                     EdgeNavigator navigator) {
        this.navigator = navigator;
        this.dependencyChecker = new NodeDependencyChecker(configuration, navigator);
        this.stateLoader = new NodeStateLoader(configuration, context);
        this.executionTracker = new ExecutionTracker();
    }

    @Override
    public AbsNode currentNode() {
        return currentNode;
    }

    /**
     * 获取下一节点列表
     *
     * @param currentNode       当前节点
     * @param currentNodeResult 当前节点执行结果
     * @return 下一节点列表
     */
    @Override
    public List<AbsNode> nextNodes(AbsNode currentNode, NodeResult currentNodeResult) {
        // 检查是否需要中断执行
        if (NodeStatus.INTERRUPT.getStatus() == currentNode.getStatus()) {
            return List.of();
        }
        // 获取下游边
        List<LfEdge> sourceEdges = navigator.findDownstreamEdges(currentNode.getId());
        if (sourceEdges.isEmpty()) {
            return List.of();
        }
        // 获取目标节点ID
        List<String> targetNodeIds = sourceEdges.stream()
                .map(LfEdge::getTargetNodeId)
                .distinct()
                .toList();

        // 处理断言结果分支
        if (NodeResultWriter.isAssertionResult(currentNodeResult)) {
            List<AbsNode> targetNodes = buildNextNodes(targetNodeIds, currentNode);
            targetNodes.forEach(node -> {
                if (!isAssertionNode(node.getId(), currentNodeResult, sourceEdges)
                        && dependencyChecker.isAssertionSkipNode(node, currentNode.getId())) {
                    node.setStatus(NodeStatus.SKIP.getStatus());
                }
            });
            return targetNodes;
        }
        return buildNextNodes(targetNodeIds, currentNode);
    }

    /**
     * 检查依赖节点是否已执行
     *
     * @param node 待检查节点
     * @return 是否所有依赖节点都已执行
     */
    @Override
    public boolean dependenciesNotExecuted(AbsNode node) {
        return dependencyChecker.dependenciesNotExecuted(node);
    }

    /**
     * 检查是否为可跳过节点（所有上游节点均为 SKIP）
     *
     * @param node 待检查节点
     * @return 是否可跳过
     */
    @Override
    public boolean isSkipNode(AbsNode node) {
        return dependencyChecker.isSkipNode(node);
    }

    /**
     * 加载节点状态
     * 用于恢复中断的工作流执行
     *
     * @param workflow        工作流实例（用于 saveContext）
     * @param details         节点详情
     * @param currentNodeId   当前节点运行时ID
     * @param currentNodeData 当前节点数据
     */
    public void loadNodeState(IWorkflow workflow, JSONObject details, String currentNodeId, Map<String, Object> currentNodeData) {
        this.currentNode = stateLoader.loadNodeState(workflow, details, currentNodeId, currentNodeData);
    }

    /**
     * 根据节点ID获取节点实例
     *
     * @param nodeId            节点ID
     * @param upNodeIds         上游节点ID列表
     * @param getNodeProperties 节点属性处理函数
     * @return 节点实例
     */
    public AbsNode getNodeInstance(String nodeId, List<String> upNodeIds, Function<AbsNode, JSONObject> getNodeProperties) {
        return stateLoader.getNodeInstance(nodeId, upNodeIds, getNodeProperties);
    }

    /**
     * 记录节点执行
     *
     * @param node 正在执行的节点
     */
    @Override
    public void recordExecution(AbsNode node) {
        executionTracker.recordExecution(node);
    }

    /**
     * 构建节点列表
     *
     * @param targetNodeIds 目标节点ID列表
     * @param currentNode   当前节点
     * @return 节点列表
     */
    private List<AbsNode> buildNextNodes(List<String> targetNodeIds, AbsNode currentNode) {
        List<String> upNodeIdList = new ArrayList<>(currentNode.getUpNodeIdList());
        upNodeIdList.add(currentNode.getId());
        List<AbsNode> nextNodes= targetNodeIds.stream()
                .map(nodeId -> getNodeInstance(nodeId, upNodeIdList, null))
                .filter(Objects::nonNull)
                .toList();
        nextNodes.forEach(n->n.setStatus(NodeStatus.READY.getStatus()));
        return nextNodes;
    }

    /**
     * 判断是否为断言节点
     *
     * @param nodeId            节点ID
     * @param currentNodeResult 当前节点执行结果
     * @param sourceEdges       下游边列表
     * @return 是否为断言节点
     */
    private boolean isAssertionNode(String nodeId, NodeResult currentNodeResult, List<LfEdge> sourceEdges) {
        List<String> assertionNodeIds = sourceEdges.stream()
                .filter(edge -> {
                    Map<String, Object> nodeVariables = currentNodeResult.getNodeVariable();
                    String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault(NodeField.BRANCH_ID, "") : "";
                    String expectedAnchorId = String.format("%s_%s_right", edge.getSourceNodeId(), branchId);
                    return expectedAnchorId.equals(edge.getSourceAnchorId());
                })
                .map(LfEdge::getTargetNodeId)
                .toList();
        return CollectionUtils.isNotEmpty(assertionNodeIds) && assertionNodeIds.contains(nodeId);
    }

}
