package com.maxkb4j.workflow.model;

import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工作流配置管理器
 * 负责管理工作流的不可变配置：模式、节点、边、聊天参数
 *
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
     */
    private final List<AbsNode> nodes;

    /**
     * 边列表（不可变）
     */
    private final List<LfEdge> edges;

    /**
     * 节点ID到节点的映射（O(1)查找）
     */
    private final Map<String, AbsNode> nodeMap;

    /**
     * 聊天参数
     */
    private ChatParams chatParams;

    /**
     * 构造器
     *
     * @param workflowMode 工作流模式
     * @param nodes        节点列表
     * @param edges        边列表
     */
    public WorkflowConfiguration(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges) {
        this.workflowMode = workflowMode;
        this.nodes = nodes != null ? Collections.unmodifiableList(nodes) : Collections.emptyList();
        this.edges = edges != null ? Collections.unmodifiableList(edges) : Collections.emptyList();
        this.nodeMap = buildNodeMap(nodes);
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
     * 设置聊天参数
     *
     * @param chatParams 聊天参数
     */
    public void setChatParams(ChatParams chatParams) {
        this.chatParams = chatParams;
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

    /**
     * 检查节点是否存在
     *
     * @param nodeId 节点ID
     * @return 是否存在
     */
    public boolean hasNode(String nodeId) {
        return nodeMap.containsKey(nodeId);
    }

    /**
     * 获取节点数量
     *
     * @return 节点数量
     */
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * 获取边数量
     *
     * @return 边数量
     */
    public int edgeCount() {
        return edges.size();
    }

}