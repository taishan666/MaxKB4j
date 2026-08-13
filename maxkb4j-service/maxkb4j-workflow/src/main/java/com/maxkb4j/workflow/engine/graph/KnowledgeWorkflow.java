package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.workflow.engine.*;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.DataSource;
import com.maxkb4j.workflow.model.IKnowledgeWorkflow;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 知识库工作流
 * 无 Sink 输出，知识库参数作为全局变量注入上下文。
 */
@Slf4j
@Getter
public class KnowledgeWorkflow extends AbstractWorkflow implements IKnowledgeWorkflow {

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
        WorkflowOutputManager outputManager = new WorkflowOutputManager(configuration, context, null);
        return new Components(configuration, context, new HistoryManager(List.of()), executionAccessor, outputManager);
    }



    @Override
    public List<AbsNode> startNodes() {
        List<AbsNode> dataSourceNodes = configuration.getNodes().stream()
                .filter(node -> Objects.nonNull(node.getType())&&node.getType().startsWith("data-source-"))
                .toList();
        DataSource dataSource = knowledgeParams.getDataSource();
        String dataSourceNodeId = dataSource != null ? dataSource.getNodeId() : null;
        if (dataSourceNodeId == null) {
            log.warn("dataSource or its nodeId is null, skip marking non-data-source start nodes as SKIP");
            return List.of();
        }
        markNonDataSourceNodesAsSkip(dataSourceNodes,dataSourceNodeId);
        return dataSourceNodes;
    }

    /**
     * 将非数据源的起始节点标记为 SKIP
     * dataSource 或其 nodeId 为空时不标记（防御 NPE）
     */
    private void markNonDataSourceNodesAsSkip(List<AbsNode> startNodes,String dataSourceNodeId) {
        for (AbsNode startNode : startNodes) {
            if (!startNode.getId().equals(dataSourceNodeId)) {
                startNode.setStatus(NodeStatus.SKIP.getStatus());
            }
        }
    }

}
