package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作流配置管理器
 * 负责管理工作流的不可变配置：模式、节点、边、聊天参数
 * 从 Workflow 类提取，遵循单一职责原则
 */
@Getter
public class WorkflowConfiguration {

    /**
     * 工作流模式
     */
    private final WorkflowMode workflowMode;

    /**
     * 节点列表（不可变）
     * 注：使用副本 + unmodifiableList 双重保护，避免外部修改底层列表
     */
    private final List<AbsNode> nodes;

    /**
     * 边列表（不可变）
     * 注：使用副本 + unmodifiableList 双重保护，避免外部修改底层列表
     */
    private final List<LfEdge> edges;

    /**
     * 节点ID到节点的映射（O(1)查找）
     */
    private final Map<String, AbsNode> nodeMap;

    /**
     * 节点执行超时时间（分钟）
     * 默认 10 分钟
     * 注意：字段名以 Minutes 结尾，单位为分钟，调用方需使用 TimeUnit.MINUTES
     */
    private final long nodeExecutionTimeoutMinutes = 10;

    /**
     * 构造器
     *
     * @param workflowMode 工作流模式
     * @param nodes        节点列表
     * @param edges        边列表
     */
    public WorkflowConfiguration(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges) {
        this.workflowMode = workflowMode;
        // 防御性复制：避免外部传入的可变列表影响配置不可变性
        List<AbsNode> nodesCopy = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
        List<LfEdge> edgesCopy = edges != null ? new ArrayList<>(edges) : new ArrayList<>();
        this.nodes = Collections.unmodifiableList(nodesCopy);
        this.edges = Collections.unmodifiableList(edgesCopy);
        this.nodeMap = buildNodeMap(nodesCopy);
    }

    /**
     * 构建节点映射
     */
    private Map<String, AbsNode> buildNodeMap(List<AbsNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return nodes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(AbsNode::getId, n -> n, (a, b) -> a));
    }


    /**
     * 根据节点ID获取节点实例
     * 从配置解析节点，注入上游节点列表与属性处理函数，并重新生成运行时ID
     *
     * @param nodeId            节点ID
     * @param upNodeIds         上游节点ID列表
     * @param getNodeProperties 节点属性处理函数
     * @return 节点实例，节点不存在时返回 null
     */
    public AbsNode getNodeInstance(String nodeId, List<String> upNodeIds, Function<AbsNode, JSONObject> getNodeProperties) {
        AbsNode node = this.getNode(nodeId);
        if (node != null) {
            node.setUpNodeIdList(upNodeIds);
            node.setStatus(NodeStatus.READY.getStatus());
            if (getNodeProperties != null) {
                getNodeProperties.apply(node);
            }
        }
        return node;
    }

    /**
     * 根据节点ID获取节点
     *
     * @param nodeId 节点ID
     * @return 节点实例，不存在返回 null
     */
    public AbsNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

}
