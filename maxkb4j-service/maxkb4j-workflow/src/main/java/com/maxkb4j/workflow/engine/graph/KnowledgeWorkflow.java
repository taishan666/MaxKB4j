package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.workflow.engine.EdgeNavigator;
import com.maxkb4j.workflow.engine.HistoryManager;
import com.maxkb4j.workflow.engine.WorkflowConfiguration;
import com.maxkb4j.workflow.engine.WorkflowContext;
import com.maxkb4j.workflow.engine.WorkflowExecutionAccessor;
import com.maxkb4j.workflow.engine.WorkflowOutputManager;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.DataSource;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库工作流
 * 无 Sink 输出，知识库参数作为全局变量注入上下文。
 */
@Slf4j
@Getter
public class KnowledgeWorkflow extends AbstractWorkflow {

    private final KnowledgeParams knowledgeParams;

    public KnowledgeWorkflow(List<AbsNode> nodes, List<LfEdge> edges, KnowledgeParams knowledgeParams) {
        super(compose(nodes, edges, knowledgeParams));
        this.knowledgeParams = knowledgeParams;
    }

    /**
     * 组装知识库工作流组件束
     */
    private static Components compose(List<AbsNode> nodes, List<LfEdge> edges, KnowledgeParams knowledgeParams) {
        Objects.requireNonNull(knowledgeParams, "knowledgeParams cannot be null");
        // 1. 初始化配置
        WorkflowConfiguration configuration = new WorkflowConfiguration(WorkflowMode.KNOWLEDGE, nodes, edges);
        // 2. 初始化上下文（知识库参数注入全局变量）
        WorkflowContext context = new WorkflowContext();
        Map<String, Object> knowledgeBase = knowledgeParams.getKnowledgeBase();
        if (knowledgeBase != null) {
            context.getGlobalContext().putAll(knowledgeBase);
        }
        // 3. 初始化执行控制器与输出管理器（知识库工作流无 Sink 输出）
        WorkflowExecutionAccessor executionAccessor = new WorkflowExecutionAccessor(configuration, context, new EdgeNavigator(edges));
        WorkflowOutputManager outputManager =
                new WorkflowOutputManager(configuration, context, null);
        return new Components(configuration, context, new HistoryManager(List.of()), executionAccessor, outputManager);
    }

    /**
     * 获取知识库工作流的起始节点列表
     * <p>
     * <b>副作用警告</b>：本方法违反 CQS（命令查询分离）原则，会修改返回节点列表中的状态--
     * 当 {@code knowledgeParams.dataSource.nodeId} 有效时，
     * 其余起始节点会被设置为 {@link NodeStatus#SKIP} 状态；
     * dataSource 或其 nodeId 为空时跳过 SKIP 标记，返回全部起始节点。
     * <p>
     * 多次调用是幂等的（重复设置 SKIP 状态不会累积副作用），
     * 但调用方仍需在确认副作用后才可调用，避免在执行过程中意外重置节点状态。
     *
     * @return 起始节点列表（非 KNOWLEDGE_BASE 类型且无入边的节点）
     */
    public List<AbsNode> getStartNodes() {
        Set<String> startNodeIds = findStartNodeIds();
        List<AbsNode> startNodes = configuration.getNodes().stream()
                .filter(node -> !NodeType.KNOWLEDGE_BASE.getKey().equals(node.getType()))
                .filter(node -> startNodeIds.contains(node.getId()))
                .toList();
        markNonDataSourceNodesAsSkip(startNodes);
        return startNodes;
    }

    /**
     * 将非数据源的起始节点标记为 SKIP
     * dataSource 或其 nodeId 为空时不标记（防御 NPE）
     */
    private void markNonDataSourceNodesAsSkip(List<AbsNode> startNodes) {
        DataSource dataSource = knowledgeParams.getDataSource();
        String dataSourceNodeId = dataSource != null ? dataSource.getNodeId() : null;
        if (dataSourceNodeId == null) {
            log.warn("dataSource or its nodeId is null, skip marking non-data-source start nodes as SKIP");
            return;
        }
        for (AbsNode startNode : startNodes) {
            if (!startNode.getId().equals(dataSourceNodeId)) {
                startNode.setStatus(NodeStatus.SKIP.getStatus());
            }
        }
    }

    /**
     * 计算起始节点 ID 集合（无入边的节点），复杂度 O(N+E)
     */
    private Set<String> findStartNodeIds() {
        Set<String> nodesWithIncomingEdge = configuration.getEdges().stream()
                .map(LfEdge::getTargetNodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return configuration.getNodes().stream()
                .map(AbsNode::getId)
                .filter(id -> !nodesWithIncomingEdge.contains(id))
                .collect(Collectors.toSet());
    }
}
