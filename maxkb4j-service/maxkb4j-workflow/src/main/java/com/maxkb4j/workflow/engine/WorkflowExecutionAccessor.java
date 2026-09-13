package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.IWorkflowExecutionAccessor;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.INode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 工作流配置（节点解析来源）
     */
    private final WorkflowConfiguration configuration;
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
        this.configuration = configuration;
        this.dependencyChecker = new NodeDependencyChecker(configuration, navigator);
        this.stateLoader = new NodeStateLoader(configuration, context);
        this.executionTracker = new ExecutionTracker();
    }

    /**
     * 提取下游边指向的目标节点ID（去重）
     *
     * @param sourceEdges 下游边列表
     * @return 目标节点ID列表
     */
    private static List<String> extractTargetNodeIds(List<LfEdge> sourceEdges) {
        return sourceEdges.stream()
                .map(LfEdge::getTargetNodeId)
                .distinct()
                .toList();
    }

    /**
     * 计算断言结果命中的目标节点ID集合
     * <p>
     * 断言结果通过 branchId 标识命中分支，对应边的锚点格式为 {sourceNodeId}_{branchId}_right
     * </p>
     *
     * @param currentNodeResult 当前节点执行结果
     * @param sourceEdges       下游边列表
     * @return 命中断言分支的目标节点ID集合
     */
    private static Set<String> findAssertionTargetNodeIds(NodeResult currentNodeResult, List<LfEdge> sourceEdges) {
        Map<String, Object> nodeVariables = currentNodeResult.getNodeVariable();
        String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault(NodeField.BRANCH_ID, "") : "";
        return sourceEdges.stream()
                .filter(edge -> (edge.getSourceNodeId() + "_" + branchId + "_right").equals(edge.getSourceAnchorId()))
                .map(LfEdge::getTargetNodeId)
                .collect(Collectors.toSet());
    }

    @Override
    public AbsNode currentNode() {
        return currentNode;
    }

    /**
     * 获取下一个节点列表
     *
     * @param currentNode       当前节点
     * @param currentNodeResult 当前节点执行结果
     * @return 下一个节点列表
     */
    @Override
    public List<INode> nextNodes(INode currentNode, NodeResult currentNodeResult) {
        // 检查是否需要中断执行
        if (currentNodeResult != null && NodeResultWriter.isInterruptExec(currentNodeResult, currentNode)) {
            return List.of();
        }
        // 获取下游边
        List<LfEdge> sourceEdges = navigator.findDownstreamEdges(currentNode.getId());
        if (sourceEdges.isEmpty()) {
            return List.of();
        }
        // 处理断言结果分支：命中分支的节点正常执行，未命中的标记 SKIP
        if (currentNodeResult != null && NodeResultWriter.isAssertionResult(currentNodeResult)) {
            Set<String> assertionNodeIds = findAssertionTargetNodeIds(currentNodeResult, sourceEdges);
            List<INode> targetNodes = buildNextNodes(extractTargetNodeIds(sourceEdges), currentNode);
            targetNodes.forEach(node -> {
                if (!assertionNodeIds.contains(node.getId())) {
                    node.setStatus(NodeStatus.SKIP.getStatus());
                }
            });
            return targetNodes;
        }
        // 普通分支：仅保留默认出口（right 锚点）的下游节点
        List<String> targetNodeIds = sourceEdges.stream()
                .filter(edge -> edge.getSourceAnchorId().equals(edge.getSourceNodeId() + "_right"))
                .map(LfEdge::getTargetNodeId)
                .distinct()
                .toList();
        return targetNodeIds.isEmpty() ? List.of() : buildNextNodes(targetNodeIds, currentNode);
    }

    /**
     * 检查依赖节点是否已执行
     *
     * @param node 待检查节点
     * @return 是否所有依赖节点都已执行
     */
    @Override
    public boolean dependenciesNotExecuted(INode node) {
        return dependencyChecker.dependenciesNotExecuted(node);
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
     * 记录节点执行
     *
     * @param node 正在执行的节点
     */
    @Override
    public void recordExecution(INode node) {
        executionTracker.recordExecution(node);
    }

    /**
     * 构建节点列表
     *
     * @param targetNodeIds 目标节点ID列表
     * @param currentNode   当前节点
     * @return 节点列表
     */
    private List<INode> buildNextNodes(List<String> targetNodeIds, INode currentNode) {
        List<String> upNodeIdList = new ArrayList<>(currentNode.getUpNodeIdList());
        upNodeIdList.add(currentNode.getId());
        List<INode> nextNodes = new ArrayList<>(targetNodeIds.size());
        for (String nodeId : targetNodeIds) {
            AbsNode node = configuration.getNodeInstance(nodeId, upNodeIdList, null);
            if (Objects.nonNull(node)) {
                nextNodes.add(node);
            }
        }
        return nextNodes;
    }

}
