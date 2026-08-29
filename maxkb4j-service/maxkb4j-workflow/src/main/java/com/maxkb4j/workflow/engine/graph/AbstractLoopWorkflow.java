package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.engine.*;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Objects;

/**
 * 循环工作流抽象基类
 * 统一聊天循环与知识库循环工作流的公共构建逻辑：
 * <ul>
 *   <li>复用父工作流的上下文（共享全局/聊天/循环变量）与历史管理器</li>
 *   <li>基于循环体节点与边构建独立的配置</li>
 * </ul>
 */
@Getter
public abstract class AbstractLoopWorkflow extends AbstractWorkflow {

    private final LoopParams loopParams;

    protected AbstractLoopWorkflow(Components components, LoopParams loopParams) {
        super(components);
        this.loopParams = Objects.requireNonNull(loopParams, "loopParams cannot be null");
    }

    /**
     * 组装循环工作流组件束
     *
     * @param parent 父工作流（复用其上下文与历史管理器）
     * @param nodes  循环内节点列表
     * @param edges  循环内边列表
     * @param sink   输出 Sink（无输出场景传 null）
     * @return 组件束
     */
    protected static Components composeLoopComponents(AbstractWorkflow parent, List<AbsNode> nodes,
                                                      List<LfEdge> edges, Sinks.Many<ChatMessageVO> sink) {
        Objects.requireNonNull(parent, "parent workflow cannot be null");
        WorkflowMode workflowMode=parent.configuration.getWorkflowMode();
        if (WorkflowMode.APPLICATION.equals(workflowMode)){
            workflowMode = WorkflowMode.APPLICATION_LOOP;
        } else if (WorkflowMode.KNOWLEDGE.equals(workflowMode)){
            workflowMode = WorkflowMode.KNOWLEDGE_LOOP;
        }
        WorkflowConfiguration configuration = new WorkflowConfiguration(workflowMode, nodes, edges);
        WorkflowContext sharedContext = new WorkflowContext(parent.workflowContext);
        EdgeNavigator navigator = new EdgeNavigator(edges);
        WorkflowExecutionAccessor executionAccessor = new WorkflowExecutionAccessor(configuration, sharedContext, navigator);
        WorkflowOutputManager outputManager = new WorkflowOutputManager(configuration, sharedContext, sink);
        return new Components(configuration, sharedContext, parent.historyManager,
                executionAccessor, outputManager);
    }


    @Override
    public List<AbsNode> startNodes() {
        return List.of(getNode(NodeType.LOOP_START.getKey()));
    }
}
