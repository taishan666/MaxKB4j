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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class LoopWorkFlow extends Workflow {

    private LoopParams loopParams;
    private Map<String, Object> loopContext;

    public LoopWorkFlow(Workflow workflow, List<AbsNode> nodes, List<LfEdge> edges,
                        LoopParams loopParams, JSONObject details, Sinks.Many<ChatMessageVO> sink) {
        // 初始化配置
        this.configuration = new WorkflowConfiguration(
                workflow.getConfiguration().getWorkflowMode(), nodes, edges);
        this.configuration.setChatParams(workflow.getConfiguration().getChatParams());

        // 复用父工作流的上下文
        this.workflowContext = workflow.getWorkflowContext();
        this.historyManager = workflow.getHistoryManager();

        // 创建包含 loop 上下文的 VariableResolver
        this.loopContext = new HashMap<>();
        this.loopParams = loopParams;
        this.variableResolver = new VariableResolver(this.workflowContext, this.loopContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);

        // 初始化边导航器
        this.edgeNavigator = new EdgeNavigator(edges);

        // 初始化执行控制器（覆盖 getStartNode 以返回 LoopStart 节点）
        this.executionController = new LoopExecutionController(
                this.configuration, this.workflowContext, this.edgeNavigator, this.templateRenderer);

        // 初始化输出管理器
        this.outputManager = new WorkflowOutputManager(
                this.configuration, this.workflowContext, sink);
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
            return getNodeInstance(NodeType.LOOP_START.getKey(), List.of(), null);
        }
    }
}