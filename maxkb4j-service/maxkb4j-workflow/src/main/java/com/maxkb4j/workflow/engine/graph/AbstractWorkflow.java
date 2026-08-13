package com.maxkb4j.workflow.engine.graph;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.engine.*;
import com.maxkb4j.workflow.model.*;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 工作流门面抽象基类
 * 提供统一的访问入口，隐藏内部组件复杂性。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>构造注入：所有内部组件通过 {@link Components} 一次性注入，字段不可变，杜绝半初始化实例。</li>
 *   <li>便捷方法优先：常用操作通过 facade 方法直接访问。</li>
 *   <li>分层访问器：细粒度控制通过 context()/execution()/output() 获取。</li>
 * </ul>
 * 使用示例：
 * <pre>
 * // 便捷方法（推荐）
 * workflow.renderPrompt("{{start.question}}");
 * workflow.getHistoryMessages(10, "all", null);
 *
 * // 分层访问器（细粒度控制）
 * workflow.context().appendNode(node);
 * workflow.execution().nextNodes(current, result);
 * workflow.output().emit(message);
 * </pre>
 */
public abstract class AbstractWorkflow implements IWorkflow {

    protected final WorkflowConfiguration configuration;
    protected final WorkflowContext workflowContext;
    protected final HistoryManager historyManager;
    protected final WorkflowExecutionAccessor executionAccessor;
    protected final WorkflowOutputManager outputManager;

    /**
     * 工作流内部组件束
     * 供子类在构造器中一次性组装各组件，解决组件间相互依赖、无法逐个传入 super() 的问题。
     */
    protected record Components(WorkflowConfiguration configuration,
                                WorkflowContext context,
                                HistoryManager historyManager,
                                WorkflowExecutionAccessor executionAccessor,
                                WorkflowOutputManager outputManager) {
    }

    protected AbstractWorkflow(Components components) {
        Objects.requireNonNull(components, "components cannot be null");
        this.configuration = Objects.requireNonNull(components.configuration(), "configuration cannot be null");
        this.workflowContext = Objects.requireNonNull(components.context(), "context cannot be null");
        this.historyManager = Objects.requireNonNull(components.historyManager(), "historyManager cannot be null");
        this.executionAccessor = Objects.requireNonNull(components.executionAccessor(), "executionAccessor cannot be null");
        this.outputManager = Objects.requireNonNull(components.outputManager(), "outputManager cannot be null");
    }

    // ==================== 便捷方法层（推荐使用） ====================

    @Override
    public IWorkflowOutputManager output() {
        return outputManager;
    }

    /**
     * 获取全局上下文
     *
     * @return 全局变量 Map
     */
    @Override
    public Map<String, Object> getGlobalContext() {
        return workflowContext.getGlobalContext();
    }

    /**
     * 获取聊天上下文
     *
     * @return 聊天变量 Map
     */
    @Override
    public Map<String, Object> getChatContext() {
        return workflowContext.getChatContext();
    }

    /**
     * 获取循环上下文
     *
     * @return 循环变量 Map
     */
    @Override
    public Map<String, Object> getLoopContext() {
        return workflowContext.getLoopContext();
    }

    /**
     * 获取历史聊天记录
     *
     * @return 历史记录列表
     */
    @Override
    public List<ChatRecordSimple> getHistoryChatRecords() {
        return historyManager.getSimpleMessages();
    }

    /**
     * 获取历史消息
     *
     * @param dialogueNumber 对话轮数
     * @param dialogueType   对话类型
     * @param runtimeNodeId  运行时节点 ID
     * @return 历史消息列表
     */
    @Override
    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        return historyManager.getHistoryMessages(dialogueNumber, dialogueType, runtimeNodeId);
    }

    /**
     * 渲染提示词模板
     *
     * @param prompt 模板字符串
     * @return 渲染后的字符串
     */
    @Override
    public String renderPrompt(String prompt) {
        return workflowContext.render(prompt);
    }

    /**
     * 渲染提示词模板（带额外变量）
     *
     * @param prompt       模板字符串
     * @param addVariables 额外变量 Map
     * @return 渲染后的字符串
     */
    @Override
    public String renderPrompt(String prompt, Map<String, Object> addVariables) {
        return workflowContext.render(prompt, addVariables);
    }

    /**
     * 获取提示词变量
     *
     * @return 变量 Map
     */
    @Override
    public Map<String, Object> getPromptVariables() {
        return workflowContext.getPromptVariables();
    }

    /**
     * 获取引用字段值
     *
     * @param reference 字段引用路径 [nodeId, fieldName]
     * @return 字段值
     */
    @Override
    public Object getReferenceField(List<String> reference) {
        return workflowContext.getReferenceField(reference);
    }

    /**
     * 获取字段值
     *
     * @param value  字段值或引用路径
     * @param source 值来源类型
     * @return 实际字段值
     */
    @Override
    public Object getFieldValue(Object value, String source) {
        return workflowContext.getFieldValue(value, source);
    }

    /**
     * 根据节点 ID 获取节点
     *
     * @param nodeId 节点 ID
     * @return 节点实例
     */
    @Override
    public AbsNode getNode(String nodeId) {
        return configuration.getNode(nodeId);
    }

    /**
     * 获取节点执行超时时间（分钟）
     * 返回值与 TimeUnit.MINUTES 配合使用
     *
     * @return 超时时间（分钟）
     */
    @Override
    public long getNodeExecutionTimeoutMinutes() {
        return configuration.getNodeExecutionTimeoutMinutes();
    }

    // ==================== 分层访问器（推荐使用） ====================

    /**
     * 获取上下文访问器
     *
     * @return ContextAccessor 实例
     */
    @Override
    public IWorkflowContext context() {
        return workflowContext;
    }

    /**
     * 获取执行访问器
     *
     * @return ExecutionAccessor 实例
     */
    @Override
    public IWorkflowExecutionAccessor execution() {
        return executionAccessor;
    }

    /**
     * 根据节点ID获取节点实例
     *
     * @param nodeId          节点ID
     * @param upNodeIds       上游节点ID列表
     * @param getNodeProperties 节点属性处理函数
     * @return 节点实例
     */
    public AbsNode getNodeInstance(String nodeId, List<String> upNodeIds, Function<AbsNode, JSONObject> getNodeProperties) {
        AbsNode node = configuration.getNode(nodeId);
        if (node != null) {
            node.setUpNodeIdList(upNodeIds);
            if (getNodeProperties != null) {
                getNodeProperties.apply(node);
            }
        }
        return node;
    }

}
