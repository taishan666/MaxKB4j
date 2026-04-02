package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.List;

/**
 * 循环工作流
 * 继承 Workflow，用于处理循环节点的执行逻辑
 *
 * 特殊设计：
 * - 复用父工作流的上下文（共享全局和聊天变量）
 * - 维护独立的循环上下文（loop 变量）
 * - 覆盖 getStartNode 以返回 LoopStart 节点
 */
@Getter
@Setter
public class LoopWorkFlow extends Workflow {

    private LoopParams loopParams;

    /**
     * 构造器（使用父工作流上下文）
     *
     * @param workflow 父工作流
     * @param nodes          循环内节点列表
     * @param edges          循环内边列表
     * @param loopParams     循环参数
     * @param details        节点详情
     * @param sink           输出 Sink
     */
    public LoopWorkFlow(Workflow workflow, List<AbsNode> nodes, List<LfEdge> edges,
                        LoopParams loopParams, JSONObject details, Sinks.Many<ChatMessageVO> sink) {
        this.loopParams = loopParams;
        // 1. 初始化配置
        this.configuration = new WorkflowConfiguration(
                workflow.configuration.getWorkflowMode(), nodes, edges);
        this.configuration.setChatParams(workflow.configuration.getChatParams());

        // 2. 复用父工作流的上下文（关键：共享上下文）
        this.workflowContext =  BeanUtil.copy(workflow.workflowContext,WorkflowContext.class);
        this.historyManager = workflow.historyManager;
        // 5. 初始化执行控制器（覆盖 getStartNode 以返回 LoopStart 节点）
        this.executionAccessor = new LoopExecutionAccessor(this.configuration, this.workflowContext, new EdgeNavigator(edges));
        if (details!=null&&!details.isEmpty()) {
            this.executionAccessor.loadNodeState(this, details,
                    workflow.getChatParams().getRuntimeNodeId(), workflow.getChatParams().getNodeData());
        }
        // 6. 初始化输出管理器
        this.outputManager = new WorkflowOutputManager(this.configuration, this.workflowContext, sink);

    }

    /**
     * 循环工作流的执行控制器
     * 覆盖 getStartNode 以返回 LoopStart 节点
     */
    private static class LoopExecutionAccessor  extends WorkflowExecutionAccessor {
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