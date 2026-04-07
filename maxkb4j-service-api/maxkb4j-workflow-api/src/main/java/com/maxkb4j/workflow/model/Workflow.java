package com.maxkb4j.workflow.model;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 工作流门面类
 * 提供统一的访问入口，隐藏内部组件复杂性
 * 设计原则：
 * - 便捷方法优先：常用操作通过 facade 方法直接访问
 * - 分层访问器：细粒度控制通过 accessor 获取
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
 * </pre>
 */
@Slf4j
public class Workflow {

    protected WorkflowConfiguration configuration;
    protected WorkflowContext workflowContext;
    protected HistoryManager historyManager;
    protected WorkflowExecutionAccessor executionAccessor;
    protected WorkflowOutputManager outputManager;
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

        this.executionAccessor = new WorkflowExecutionAccessor(
                this.configuration, this.workflowContext, builder.navigator);
        this.outputManager = new WorkflowOutputManager(
                this.configuration, this.workflowContext, builder.sink);
        // 3. 加载节点状态（恢复执行）
        if (builder.restoreState) {
            this.executionAccessor.loadNodeState(this, builder.details,
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
        this.executionAccessor = null;
        this.outputManager = null;
    }

    // ==================== 便捷方法层（推荐使用） ====================

    public WorkflowOutputManager output() {
        return outputManager;
    }

    /**
     * 获取全局上下文
     *
     * @return 全局变量 Map
     */
    public Map<String, Object> getGlobalContext() {
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
     * 获取聊天上下文
     *
     * @return 聊天变量 Map
     */
    public Map<String, Object> getLoopContext() {
        return workflowContext.getLoopContext();
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
        return workflowContext.render(prompt);
    }

    /**
     * 渲染提示词模板（带额外变量）
     *
     * @param prompt       模板字符串
     * @param addVariables 额外变量 Map
     * @return 渲染后的字符串
     */
    public String renderPrompt(String prompt, Map<String, Object> addVariables) {
        return workflowContext.render(prompt, addVariables);
    }

    /**
     * 获取提示词变量
     *
     * @return 变量 Map
     */
    public Map<String, Object> getPromptVariables() {
        return workflowContext.getPromptVariables();
    }

    /**
     * 获取引用字段值
     *
     * @param reference 字段引用路径 [nodeId, fieldName]
     * @return 字段值
     */
    public Object getReferenceField(List<String> reference) {
        if (CollectionUtils.isNotEmpty(reference) && reference.size() > 1) {
            return workflowContext.getReferenceField(reference.get(0), reference.get(1));
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
            return workflowContext.getReferenceField(fields.get(0), fields.get(1));
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

    /**
     * 获取聊天参数
     *
     * @return ChatParams 实例
     */
    public ChatParams getChatParams() {
        return configuration.getChatParams();
    }

    /**
     * 获取节点执行超时时间（秒）
     *
     * @return 超时时间
     */
    public long getNodeExecutionTimeoutSeconds() {
        return configuration.getNodeExecutionTimeoutSeconds();
    }

    /**
     * 获取工作流模式
     *
     * @return 工作流模式
     */
    public WorkflowMode getWorkflowMode() {
        return configuration.getWorkflowMode();
    }

    // ==================== 分层访问器（推荐使用） ====================

    /**
     * 获取上下文访问器
     *
     * @return ContextAccessor 实例
     */
    public WorkflowContext context() {
        return workflowContext;
    }

    /**
     * 获取执行访问器
     *
     * @return ExecutionAccessor 实例
     */
    public WorkflowExecutionAccessor execution() {
        return executionAccessor;
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