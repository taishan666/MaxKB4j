package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

/**
 * 工作流执行控制器
 * 负责管理工作流的执行控制：节点状态恢复、下一节点计算、依赖检查等
 *
 * 从 Workflow 类提取，遵循单一职责原则
 */
@Slf4j
@Getter
public class WorkflowExecutionAccessor {

    /**
     * 工作流配置
     */
    private final WorkflowConfiguration configuration;

    /**
     * 工作流上下文
     */
    private final WorkflowContext context;

    /**
     * 边导航器
     */
    private final EdgeNavigator navigator;

    /**
     * 模板渲染器
     */
    private final TemplateRenderer templateRenderer;



    /**
     * 当前执行节点
     */
    private AbsNode currentNode;

    /**
     * 执行路径记录
     * 记录已执行节点的 runtimeNodeId 顺序
     */
    private final List<String> executionPath;

    /**
     * 执行时间戳记录
     * Key: runtimeNodeId, Value: 执行开始时间戳（毫秒）
     */
    private final Map<String, Long> executionTimestamps;

    public WorkflowExecutionAccessor(WorkflowConfiguration configuration,
                                     WorkflowContext context,
                                     EdgeNavigator navigator,
                                     TemplateRenderer templateRenderer) {
        this.configuration = configuration;
        this.context = context;
        this.navigator = navigator;
        this.templateRenderer = templateRenderer;
        this.executionPath = new ArrayList<>();
        this.executionTimestamps = new LinkedHashMap<>();
    }

    public AbsNode currentNode() {
        return currentNode;
    }

    /**
     * 获取开始节点
     *
     * @return 开始节点实例
     */
    public AbsNode startNode() {
        return getNodeInstance(NodeType.START.getKey(), List.of(), null);
    }

    /**
     * 获取下一节点列表
     *
     * @param currentNode    当前节点
     * @param currentNodeResult 当前节点执行结果
     * @return 下一节点列表
     */
    public List<AbsNode> nextNodes(AbsNode currentNode, NodeResult currentNodeResult) {
        // 检查是否需要中断执行
        if (currentNodeResult == null || currentNodeResult.isInterruptExec(currentNode)) {
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
        if (currentNodeResult.isAssertionResult()) {
            List<AbsNode> targetNodes = buildNodes(targetNodeIds, currentNode);
            targetNodes.forEach(node -> {
                if (!isAssertionNode(node.getId(), currentNodeResult, sourceEdges)) {
                    node.setStatus(NodeStatus.SKIP.getStatus());
                }
            });
            return targetNodes;
        } else {
            return buildNodes(targetNodeIds, currentNode);
        }
    }

    /**
     * 检查依赖节点是否已执行
     *
     * @param node 待检查节点
     * @return 是否所有依赖节点都已执行
     */
    public boolean dependenciesExecuted(AbsNode node) {
        List<String> upNodeIdList = navigator.findUpstreamNodeIds(node.getId());
        // 开始节点无上游依赖，直接通过
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return true;
        }
        Set<String> upNodeIdSet = new HashSet<>(upNodeIdList);
        return configuration.getNodes().stream()
                .filter(n -> upNodeIdSet.contains(n.getId()))
                .allMatch(n -> NodeStatus.SUCCESS.getStatus() == n.getStatus() || NodeStatus.SKIP.getStatus() == n.getStatus());
    }

    /**
     * 检查是否为就绪的 Join 节点
     * Join节点需要等待所有上游节点执行完成
     *
     * @param node 待检查节点
     * @return 是否为就绪的 Join 节点
     */
    public boolean isReadyJoin(AbsNode node) {
        List<String> upNodeIdList = navigator.findUpstreamNodeIds(node.getId());
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return false;
        }
        // 多个上游节点时，检查是否所有上游节点都是 SKIP（排除这种情况）
        if (upNodeIdList.size() > 1) {
            Set<String> upNodeIdSet = new HashSet<>(upNodeIdList);
            return !configuration.getNodes().stream()
                    .filter(n -> upNodeIdSet.contains(n.getId()))
                    .allMatch(n -> NodeStatus.SKIP.getStatus() == n.getStatus());
        }
        return false;
    }

    /**
     * 加载节点状态
     * 用于恢复中断的工作流执行
     *
     * @param workflow       工作流实例（用于 saveContext）
     * @param details        节点详情
     * @param currentNodeId  当前节点运行时ID
     * @param currentNodeData 当前节点数据
     */
    @SuppressWarnings("unchecked")
    public void loadNodeState(Workflow workflow, JSONObject details, String currentNodeId, Map<String, Object> currentNodeData) {
        if (details == null || currentNodeId == null) {
            log.warn("loadNodeState called with null details or currentNodeId");
            return;
        }
        List<Map<String, Object>> sortedDetails = details.values().stream()
                .filter(Objects::nonNull)
                .map(row -> (Map<String, Object>) row)
                .sorted(Comparator.comparing(
                        e -> (Integer) e.get("index"),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        for (Map<String, Object> nodeDetail : sortedDetails) {
            String nodeId = (String) nodeDetail.get("nodeId");
            List<String> upNodeIdList = (List<String>) nodeDetail.get("upNodeIdList");
            String runtimeNodeId = (String) nodeDetail.get("runtimeNodeId");
            Integer nodeStatus = (Integer) nodeDetail.get("status");
            if (runtimeNodeId.equals(currentNodeId)) {
                // 处理当前节点
                this.currentNode = getNodeInstance(nodeId, upNodeIdList, n -> {
                    JSONObject nodeProperties = n.getProperties();
                    if (nodeProperties.containsKey("nodeData")) {
                        JSONObject nodeParams = nodeProperties.getJSONObject("nodeData");
                        nodeParams.put("form_data", currentNodeData);
                    }
                    return nodeProperties;
                });
                if (currentNode != null) {
                    currentNode.setStatus(nodeStatus);
                    currentNode.saveContext(workflow, nodeDetail);
                    currentNode.setDetail(nodeDetail);
                    context.appendNode(currentNode);
                }
            } else {
                // 处理其他节点
                AbsNode node = getNodeInstance(nodeId, upNodeIdList, null);
                if (node != null) {
                    node.setStatus(nodeStatus);
                    node.saveContext(workflow, nodeDetail);
                    node.setDetail(nodeDetail);
                    context.appendNode(node);
                }
            }
        }
    }

    /**
     * 根据节点ID获取节点实例
     *
     * @param nodeId          节点ID
     * @param upNodeIds       上游节点ID列表
     * @param getNodeProperties 节点属性处理函数
     * @return 节点实例
     */
    public AbsNode getNodeInstance(String nodeId, List<String> upNodeIds, Function<AbsNode, JSONObject> getNodeProperties) {
        AbsNode node = configuration.getNode(nodeId);
        if (node != null) {
            node.setUpNodeIdList(upNodeIds);
            node.setTemplateRenderer(templateRenderer);
            if (getNodeProperties != null) {
                getNodeProperties.apply(node);
            }
        }
        return node;
    }

    /**
     * 构建节点列表
     *
     * @param targetNodeIds 目标节点ID列表
     * @param currentNode   当前节点
     * @return 节点列表
     */
    private List<AbsNode> buildNodes(List<String> targetNodeIds, AbsNode currentNode) {
        List<String> upNodeIdList = new ArrayList<>(currentNode.getUpNodeIdList());
        upNodeIdList.add(currentNode.getId());
        return targetNodeIds.stream()
                .map(nodeId -> getNodeInstance(nodeId, upNodeIdList, null))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 判断是否为断言节点
     *
     * @param nodeId           节点ID
     * @param currentNodeResult 当前节点执行结果
     * @param sourceEdges      下游边列表
     * @return 是否为断言节点
     */
    private boolean isAssertionNode(String nodeId, NodeResult currentNodeResult, List<LfEdge> sourceEdges) {
        List<String> assertionNodeIds = sourceEdges.stream()
                .filter(edge -> {
                    Map<String, Object> nodeVariables = currentNodeResult.getNodeVariable();
                    String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault("branchId", "") : "";
                    String expectedAnchorId = String.format("%s_%s_right", edge.getSourceNodeId(), branchId);
                    return expectedAnchorId.equals(edge.getSourceAnchorId());
                })
                .map(LfEdge::getTargetNodeId)
                .toList();
        return CollectionUtils.isNotEmpty(assertionNodeIds) && assertionNodeIds.contains(nodeId);
    }

    // ==================== 执行追踪功能 ====================

    /**
     * 记录节点执行
     *
     * @param node 正在执行的节点
     */
    public void recordExecution(AbsNode node) {
        if (node == null || node.getRuntimeNodeId() == null) {
            return;
        }
        String runtimeNodeId = node.getRuntimeNodeId();
        executionPath.add(runtimeNodeId);
        executionTimestamps.put(runtimeNodeId, System.currentTimeMillis());
        log.debug("Recorded execution: {} at {}", runtimeNodeId, System.currentTimeMillis());
    }

    /**
     * 获取执行轨迹
     *
     * @return 执行轨迹信息
     */
    public ExecutionTrace getExecutionTrace() {
        return new ExecutionTrace(
                Collections.unmodifiableList(executionPath),
                Collections.unmodifiableMap(executionTimestamps)
        );
    }

    /**
     * 执行轨迹信息 record
     */
    public record ExecutionTrace(List<String> path, Map<String, Long> timestamps) {
        /**
         * 获取执行节点数量
         */
        public int size() {
            return path.size();
        }

        /**
         * 获取指定节点的执行时间
         *
         * @param runtimeNodeId 运行时节点ID
         * @return 执行时间戳，不存在返回 null
         */
        public Long getExecutionTime(String runtimeNodeId) {
            return timestamps.get(runtimeNodeId);
        }

        /**
         * 计算总执行时长（毫秒）
         *
         * @return 总执行时长，如果路径为空返回 0
         */
        public long totalDuration() {
            if (path.isEmpty()) {
                return 0;
            }
            Long first = timestamps.get(path.get(0));
            Long last = timestamps.get(path.get(path.size() - 1));
            if (first == null || last == null) {
                return 0;
            }
            return last - first;
        }
    }
}