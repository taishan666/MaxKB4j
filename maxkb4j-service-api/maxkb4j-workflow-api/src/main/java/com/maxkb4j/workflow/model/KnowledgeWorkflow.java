package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class KnowledgeWorkflow extends Workflow {

    private KnowledgeParams knowledgeParams;

    public KnowledgeWorkflow(List<AbsNode> nodes, List<LfEdge> edges, KnowledgeParams knowledgeParams) {
        // 调用父类保护构造器
        super();

        this.knowledgeParams = knowledgeParams;

        // 1. 初始化配置
        this.configuration = new WorkflowConfiguration(WorkflowMode.KNOWLEDGE, nodes, edges);

        // 2. 初始化上下文
        this.workflowContext = new WorkflowContext();
        Map<String, Object> knowledgeBase = knowledgeParams.getKnowledgeBase();
        if (knowledgeBase != null) {
            this.workflowContext.getGlobalContext().putAll(knowledgeBase);
        }

        // 3. 初始化历史管理器
        this.historyManager = new HistoryManager(Collections.emptyList());

        // 4. 初始化执行控制器
        this.executionAccessor = new WorkflowExecutionAccessor(this.configuration, this.workflowContext, new EdgeNavigator(edges));

        // 5. 初始化输出管理器
        this.outputManager = new WorkflowOutputManager(this.configuration, this.workflowContext, null);
    }

    /**
     * 获取知识库工作流的起始节点列表
     * <p>
     * <b>副作用警告</b>：本方法违反 CQS（命令查询分离）原则，会修改返回节点列表中的状态——
     * 除 {@code knowledgeParams.dataSource.nodeId} 指定的数据源节点外，
     * 其余起始节点会被设置为 {@link NodeStatus#SKIP} 状态。
     * <p>
     * 多次调用是幂等的（重复设置 SKIP 状态不会累积副作用），
     * 但调用方仍需在确认副作用后才可调用，避免在执行过程中意外重置节点状态。
     *
     * @return 起始节点列表（非 KNOWLEDGE_BASE 类型且无入边的节点）
     */
    public List<AbsNode> getStartNodes() {
        String nodeId = knowledgeParams.getDataSource().getNodeId();
        List<AbsNode> workflowNodes=this.configuration.getNodes();
        List<AbsNode> startNodes = workflowNodes.stream()
                .filter(e-> !NodeType.KNOWLEDGE_BASE.getKey().equals(e.getType()))
                .filter(e -> isStartNode(e.getId()))
                .toList();
        for (AbsNode startNode : startNodes) {
            if (!startNode.getId().equals(nodeId)) {
                startNode.setStatus(NodeStatus.SKIP.getStatus());
            }
        }
        return startNodes;
    }

    public boolean isStartNode(String nodeId) {
        return this.configuration.getEdges().stream()
                .noneMatch(e -> e.getTargetNodeId().equals(nodeId));
    }
}