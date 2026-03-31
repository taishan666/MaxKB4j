package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.data.message.ChatMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流门面类
 * 提供统一的访问入口，隐藏内部组件复杂性
 *
 * 设计原则：
 * - 便捷方法优先：常用操作通过 facade 方法直接访问
 * - 分层访问器：细粒度控制通过 accessor 获取
 * - 原始组件隔离：仅测试/扩展场景访问原始组件
 *
 * 使用示例：
 * <pre>
 * // 便捷方法（推荐）
 * workflow.renderPrompt("{{start.question}}");
 * workflow.getHistoryMessages(10, "all", null);
 *
 * // 分层访问器（细粒度控制）
 * workflow.context().setGlobal("key", value);
 * workflow.execution().setCurrentNode(node);
 * workflow.output().emit(message);
 *
 * // 原始组件（仅测试用）
 * workflow.raw().context().getGlobalContext();
 * </pre>
 */
@Slf4j
public class Workflow {

    // ==================== 内部组件（protected 供子类访问） ====================

    protected WorkflowConfiguration configuration;
    protected WorkflowContext workflowContext;
    protected HistoryManager historyManager;
    protected VariableResolver variableResolver;
    protected TemplateRenderer templateRenderer;
    protected EdgeNavigator edgeNavigator;
    protected WorkflowExecutionController executionController;
    protected WorkflowOutputManager outputManager;

    // ==================== 分层访问器 ====================

    protected ContextAccessor contextAccessor;
    protected ExecutionAccessor executionAccessor;
    protected OutputAccessor outputAccessor;

    // ==================== 构造器 ====================

    /**
     * 使用 Builder 构建（推荐）
     *
     * @param builder WorkflowBuilder 实例
     */
    Workflow(WorkflowBuilder builder) {
        // 1. 基础组件（从 builder 获取）
        this.configuration = builder.configuration;
        this.workflowContext = builder.context;
        this.historyManager = builder.historyManager;
        this.edgeNavigator = builder.navigator;

        // 2. 依赖组件初始化（顺序敏感）
        this.variableResolver = new VariableResolver(this.workflowContext, builder.loopContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);
        this.executionController = new WorkflowExecutionController(
                this.configuration, this.workflowContext, this.edgeNavigator, this.templateRenderer);
        this.outputManager = new WorkflowOutputManager(
                this.configuration, this.workflowContext, builder.sink);

        // 3. 初始化访问器
        this.contextAccessor = new ContextAccessor(this.workflowContext);
        this.executionAccessor = new ExecutionAccessor(this.executionController);
        this.outputAccessor = new OutputAccessor(this.outputManager);

        // 4. 加载节点状态（恢复执行）
        if (builder.restoreState) {
            this.executionController.loadNodeState(this, builder.details,
                    builder.currentNodeId, builder.currentNodeData);
        }
    }

    /**
     * 保护构造器（供子类使用）
     * 子类可以完全控制初始化过程
     */
    protected Workflow() {
        // 子类需要自行初始化所有组件
        this.configuration = null;
        this.workflowContext = null;
        this.historyManager = null;
        this.variableResolver = null;
        this.templateRenderer = null;
        this.edgeNavigator = null;
        this.executionController = null;
        this.outputManager = null;
        this.contextAccessor = null;
        this.executionAccessor = null;
        this.outputAccessor = null;
    }

    // ==================== 向后兼容构造器（保留但标记废弃） ====================

    /**
     * 向后兼容构造器
     *
     * @param workflowMode 工作流模式
     * @param nodes        节点列表
     * @param edges        边列表
     * @deprecated 使用 WorkflowBuilder 代替
     */
    @Deprecated
    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges) {
        // 直接初始化（不使用 builder）
        this.configuration = new WorkflowConfiguration(workflowMode, nodes, edges);
        this.workflowContext = new WorkflowContext();
        this.historyManager = new HistoryManager(Collections.emptyList());
        this.edgeNavigator = new EdgeNavigator(edges);
        this.variableResolver = new VariableResolver(this.workflowContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);
        this.executionController = new WorkflowExecutionController(
                this.configuration, this.workflowContext, this.edgeNavigator, this.templateRenderer);
        this.outputManager = new WorkflowOutputManager(this.configuration, this.workflowContext, null);
        this.contextAccessor = new ContextAccessor(this.workflowContext);
        this.executionAccessor = new ExecutionAccessor(this.executionController);
        this.outputAccessor = new OutputAccessor(this.outputManager);
    }

    /**
     * 向后兼容构造器
     *
     * @deprecated 使用 WorkflowBuilder 代替
     */
    @Deprecated
    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges,
                    ChatParams chatParams, Sinks.Many<com.maxkb4j.common.domain.dto.ChatMessageVO> sink) {
        this.configuration = new WorkflowConfiguration(workflowMode, nodes, edges);
        this.configuration.setChatParams(chatParams);
        this.workflowContext = new WorkflowContext();
        this.historyManager = new HistoryManager(
                chatParams != null ? chatParams.getHistoryChatRecords() : Collections.emptyList());
        this.edgeNavigator = new EdgeNavigator(edges);
        this.variableResolver = new VariableResolver(this.workflowContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);
        this.executionController = new WorkflowExecutionController(
                this.configuration, this.workflowContext, this.edgeNavigator, this.templateRenderer);
        this.outputManager = new WorkflowOutputManager(this.configuration, this.workflowContext, sink);
        this.contextAccessor = new ContextAccessor(this.workflowContext);
        this.executionAccessor = new ExecutionAccessor(this.executionController);
        this.outputAccessor = new OutputAccessor(this.outputManager);
    }

    /**
     * 向后兼容构造器
     *
     * @deprecated 使用 WorkflowBuilder 代替
     */
    @Deprecated
    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges,
                    ChatParams chatParams, JSONObject details,
                    Sinks.Many<com.maxkb4j.common.domain.dto.ChatMessageVO> sink) {
        this.configuration = new WorkflowConfiguration(workflowMode, nodes, edges);
        this.configuration.setChatParams(chatParams);
        this.workflowContext = new WorkflowContext();
        this.historyManager = new HistoryManager(
                chatParams != null ? chatParams.getHistoryChatRecords() : Collections.emptyList());
        this.edgeNavigator = new EdgeNavigator(edges);
        this.variableResolver = new VariableResolver(this.workflowContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);
        this.executionController = new WorkflowExecutionController(
                this.configuration, this.workflowContext, this.edgeNavigator, this.templateRenderer);
        this.outputManager = new WorkflowOutputManager(this.configuration, this.workflowContext, sink);
        this.contextAccessor = new ContextAccessor(this.workflowContext);
        this.executionAccessor = new ExecutionAccessor(this.executionController);
        this.outputAccessor = new OutputAccessor(this.outputManager);
        // 加载节点状态
        if (chatParams != null && chatParams.getRuntimeNodeId() != null && details != null) {
            this.executionController.loadNodeState(this, details,
                    chatParams.getRuntimeNodeId(),
                    chatParams != null && chatParams.getChatRecord() != null
                            ? chatParams.getChatRecord().getDetails() : null);
        }
    }

    // ==================== 便捷方法层（推荐使用） ====================

    /**
     * 获取全局上下文
     *
     * @return 全局变量 Map
     */
    public Map<String, Object> getContext() {
        return workflowContext.getGlobalContext();
    }

    /**
     * 获取聊天上下文
     *
     * @return 聊天变量 Map
     */
    public Map<String, Object> getChatContext() {
        return workflowContext.getChatContext();
    }

    /**
     * 获取历史聊天记录
     *
     * @return 历史记录列表
     */
    public List<ChatRecordDTO> getHistoryChatRecords() {
        return historyManager.historyChatRecords();
    }

    /**
     * 获取历史消息
     *
     * @param dialogueNumber 对话轮数
     * @param dialogueType   对话类型
     * @param runtimeNodeId  运行时节点 ID
     * @return 历史消息列表
     */
    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        return historyManager.getHistoryMessages(dialogueNumber, dialogueType, runtimeNodeId);
    }

    /**
     * 渲染提示词模板
     *
     * @param prompt 模板字符串
     * @return 渲染后的字符串
     */
    public String renderPrompt(String prompt) {
        return templateRenderer.render(prompt);
    }

    /**
     * 获取引用字段值
     *
     * @param reference 字段引用路径 [nodeId, fieldName]
     * @return 字段值
     */
    public Object getReferenceField(List<String> reference) {
        if (CollectionUtils.isNotEmpty(reference) && reference.size() > 1) {
            return variableResolver.getReferenceField(reference.get(0), reference.get(1));
        }
        return null;
    }

    /**
     * 获取字段值
     *
     * @param value  字段值或引用路径
     * @param source 值来源类型
     * @return 实际字段值
     */
    @SuppressWarnings("unchecked")
    public Object getFieldValue(Object value, String source) {
        if ("reference".equals(source) && value instanceof List) {
            List<String> fields = (List<String>) value;
            return variableResolver.getReferenceField(fields.get(0), fields.get(1));
        }
        return value;
    }

    /**
     * 根据节点 ID 获取节点
     *
     * @param nodeId 节点 ID
     * @return 节点实例
     */
    public AbsNode getNode(String nodeId) {
        return configuration.getNode(nodeId);
    }

    // ==================== 分层访问器（推荐使用） ====================

    /**
     * 获取上下文访问器
     *
     * @return ContextAccessor 实例
     */
    public ContextAccessor context() {
        return contextAccessor;
    }

    /**
     * 获取执行访问器
     *
     * @return ExecutionAccessor 实例
     */
    public ExecutionAccessor execution() {
        return executionAccessor;
    }

    /**
     * 获取输出访问器
     *
     * @return OutputAccessor 实例
     */
    public OutputAccessor output() {
        return outputAccessor;
    }

    // ==================== 原始组件访问（仅测试/扩展用，已废弃） ====================

    /**
     * 获取工作流配置
     *
     * @return WorkflowConfiguration 实例
     * @deprecated 使用 workflow.execution().getNode() 或 workflow.getNode() 代替
     */
    @Deprecated
    public WorkflowConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * 获取工作流上下文
     *
     * @return WorkflowContext 实例
     * @deprecated 使用 workflow.context() 访问器代替
     */
    @Deprecated
    public WorkflowContext getWorkflowContext() {
        return workflowContext;
    }

    /**
     * 获取历史管理器
     *
     * @return HistoryManager 实例
     * @deprecated 使用 workflow.getHistoryMessages() 便捷方法代替
     */
    @Deprecated
    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    /**
     * 获取变量解析器
     *
     * @return VariableResolver 实例
     * @deprecated 使用 workflow.getReferenceField() 或 workflow.renderPrompt() 代替
     */
    @Deprecated
    public VariableResolver getVariableResolver() {
        return variableResolver;
    }

    /**
     * 获取模板渲染器
     *
     * @return TemplateRenderer 实例
     * @deprecated 使用 workflow.renderPrompt() 代替
     */
    @Deprecated
    public TemplateRenderer getTemplateRenderer() {
        return templateRenderer;
    }

    /**
     * 获取边导航器
     *
     * @return EdgeNavigator 实例
     * @deprecated 内部组件，不推荐直接访问
     */
    @Deprecated
    public EdgeNavigator getEdgeNavigator() {
        return edgeNavigator;
    }

    /**
     * 获取执行控制器
     *
     * @return WorkflowExecutionController 实例
     * @deprecated 使用 workflow.execution() 访问器代替
     */
    @Deprecated
    public WorkflowExecutionController getExecutionController() {
        return executionController;
    }

    /**
     * 获取输出管理器
     *
     * @return WorkflowOutputManager 实例
     * @deprecated 使用 workflow.output() 访问器代替
     */
    @Deprecated
    public WorkflowOutputManager getOutputManager() {
        return outputManager;
    }

    // ==================== 原始组件容器（仅测试用） ====================

    /**
     * 获取原始组件容器
     * 仅用于测试或特殊扩展场景
     *
     * @return WorkflowComponents 实例
     */
    public WorkflowComponents raw() {
        return new WorkflowComponents(this);
    }

    /**
     * 原始组件容器 record
     * 用于一次性获取所有原始组件（测试用）
     */
    public record WorkflowComponents(
            WorkflowConfiguration configuration,
            WorkflowContext context,
            HistoryManager historyManager,
            VariableResolver variableResolver,
            TemplateRenderer templateRenderer,
            EdgeNavigator navigator,
            WorkflowExecutionController executor,
            WorkflowOutputManager output
    ) {
        WorkflowComponents(Workflow wf) {
            this(wf.configuration, wf.workflowContext, wf.historyManager,
                 wf.variableResolver, wf.templateRenderer, wf.edgeNavigator,
                 wf.executionController, wf.outputManager);
        }
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建 WorkflowBuilder
     *
     * @param mode  工作流模式
     * @param nodes 节点列表
     * @param edges 边列表
     * @return WorkflowBuilder 实例
     */
    public static WorkflowBuilder builder(WorkflowMode mode, List<AbsNode> nodes, List<LfEdge> edges) {
        return WorkflowBuilder.create(mode, nodes, edges);
    }
}