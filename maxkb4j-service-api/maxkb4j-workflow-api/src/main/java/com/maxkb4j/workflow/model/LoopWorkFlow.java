package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<String, Object> loopContext;

    /**
     * 构造器（使用父工作流上下文）
     *
     * @param parentWorkflow 父工作流
     * @param nodes          循环内节点列表
     * @param edges          循环内边列表
     * @param loopParams     循环参数
     * @param details        节点详情
     * @param sink           输出 Sink
     */
    public LoopWorkFlow(Workflow parentWorkflow, List<AbsNode> nodes, List<LfEdge> edges,
                        LoopParams loopParams, JSONObject details, Sinks.Many<ChatMessageVO> sink) {
        // 调用父类保护构造器
        super();

        this.loopParams = loopParams;
        this.loopContext = new HashMap<>();

        // 1. 初始化配置
        this.configuration = new WorkflowConfiguration(
                parentWorkflow.configuration.getWorkflowMode(), nodes, edges);
        this.configuration.setChatParams(parentWorkflow.configuration.getChatParams());

        // 2. 复用父工作流的上下文（关键：共享上下文）
        this.workflowContext = parentWorkflow.workflowContext;
        this.historyManager = parentWorkflow.historyManager;

        // 3. 创建包含 loop 上下文的 VariableResolver
        this.variableResolver = new VariableResolver(this.workflowContext, this.loopContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);

        // 4. 初始化边导航器
        this.edgeNavigator = new EdgeNavigator(edges);

        // 5. 初始化执行控制器（覆盖 getStartNode 以返回 LoopStart 节点）
        this.executionController = new LoopExecutionController(
                this.configuration, this.workflowContext, this.edgeNavigator, this.templateRenderer);

        // 6. 初始化输出管理器
        this.outputManager = new WorkflowOutputManager(
                this.configuration, this.workflowContext, sink);

        // 7. 初始化访问器
        this.contextAccessor = new ContextAccessor(this.workflowContext);
        this.executionAccessor = new ExecutionAccessor(this.executionController);
        this.outputAccessor = new OutputAccessor(this.outputManager);
    }

    /**
     * 循环工作流的执行控制器
     * 覆盖 getStartNode 以返回 LoopStart 节点
     */
    private static class LoopExecutionController extends WorkflowExecutionController {
        public LoopExecutionController(WorkflowConfiguration configuration,
                                       WorkflowContext context,
                                       EdgeNavigator navigator,
                                       TemplateRenderer templateRenderer) {
            super(configuration, context, navigator, templateRenderer);
        }

        @Override
        public AbsNode getStartNode() {
            return getNodeInstance(NodeType.LOOP_START.getKey(), Collections.emptyList(), null);
        }
    }
}