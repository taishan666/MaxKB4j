package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.enums.NodeStatus;
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

        // 4. 初始化变量解析器和模板渲染器
        this.variableResolver = new VariableResolver(this.workflowContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);

        // 5. 初始化边导航器
        this.edgeNavigator = new EdgeNavigator(edges);

        // 6. 初始化执行控制器
        this.executionController = new WorkflowExecutionController(
                this.configuration, this.workflowContext, this.edgeNavigator, this.templateRenderer);

        // 7. 初始化输出管理器
        this.outputManager = new WorkflowOutputManager(this.configuration, this.workflowContext, null);

        // 8. 初始化访问器
        this.contextAccessor = new ContextAccessor(this.workflowContext);
        this.executionAccessor = new ExecutionAccessor(this.executionController);
        this.outputAccessor = new OutputAccessor(this.outputManager);
    }

    public List<AbsNode> getStartNodes() {
        String nodeId = knowledgeParams.getDataSource().getNodeId();
        List<AbsNode> startNodes = this.configuration.getNodes().stream()
                .filter(e -> !isTargetNode(e.getId()))
                .toList();
        for (AbsNode startNode : startNodes) {
            if (!startNode.getId().equals(nodeId)) {
                startNode.setStatus(NodeStatus.SKIP.getStatus());
            }
        }
        return startNodes;
    }

    public boolean isTargetNode(String nodeId) {
        return this.configuration.getEdges().stream()
                .anyMatch(e -> e.getTargetNodeId().equals(nodeId));
    }
}