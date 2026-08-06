package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.workflow.engine.*;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 循环工作流
 * 继承 WorkflowImpl，用于处理循环节点的执行逻辑
 * <p>
 * 特殊设计：
 * - 复用父工作流的上下文（共享全局和聊天变量）
 * - 维护独立的循环上下文（loop 变量）
 * - 覆盖 startNode 以返回 LoopStart 节点
 */
@Getter
@Setter
public class KnowledgeLoopWorkFlow extends WorkflowImpl implements ILoopWorkFlow{

    private LoopParams loopParams;

    /**
     * 构造器（使用父工作流上下文）
     *
     * @param workflow   父工作流
     * @param nodes      循环内节点列表
     * @param edges      循环内边列表
     * @param loopParams 循环参数
     */
    public KnowledgeLoopWorkFlow(KnowledgeWorkflow workflow, List<AbsNode> nodes, List<LfEdge> edges, LoopParams loopParams) {
        this.loopParams = loopParams;
        // 1. 初始化配置
        this.configuration = new WorkflowConfiguration(workflow.configuration.getWorkflowMode(), nodes, edges);

        // 2. 复用父工作流的上下文（关键：共享上下文）
        this.workflowContext = workflow.workflowContext;
        this.historyManager = workflow.historyManager;

        // 3. 初始化执行控制器（覆盖 startNode 以返回 LoopStart 节点）
        this.executionAccessor = new LoopExecutionAccessor(this.configuration, this.workflowContext, new EdgeNavigator(edges));

        this.outputManager = new WorkflowOutputManager(this.configuration, this.workflowContext, null);
    }

    /**
     * 循环工作流的执行控制器
     * 覆盖 startNode 以返回 LoopStart 节点
     */
    private static class LoopExecutionAccessor extends WorkflowExecutionAccessor {
        public LoopExecutionAccessor(WorkflowConfiguration configuration,
                                     WorkflowContext context,
                                     EdgeNavigator navigator) {
            super(configuration, context, navigator);
        }

        @Override
        public AbsNode startNode() {
            return getNodeInstance(NodeType.LOOP_START.getKey(), Collections.emptyList(), null);
        }
    }
}
