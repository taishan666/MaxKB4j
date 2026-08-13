package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.Map;

/**
 * 工作流门面接口（契约层）。
 * <p>
 * 提供统一的访问入口，隐藏内部组件复杂性。具体实现 {@code AbstractWorkflow} 位于 workflow 实现模块，
 * 通过 {@link com.maxkb4j.workflow.service.WorkflowFactory} 构造。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>便捷方法优先：常用操作（渲染模板、历史消息、引用字段等）通过本接口直接访问。</li>
 *   <li>分层访问器：细粒度控制通过 {@link #context()}、{@link #execution()}、{@link #output()} 获取。</li>
 * </ul>
 */
public interface IWorkflow {

    /**
     * 输出访问器。
     */
    IWorkflowOutputManager output();

    /**
     * 上下文访问器。
     */
    IWorkflowContext context();

    /**
     * 执行访问器。
     */
    IWorkflowExecutionAccessor execution();

    /**
     * 全局变量上下文。
     */
    Map<String, Object> getGlobalContext();

    /**
     * 聊天变量上下文。
     */
    Map<String, Object> getChatContext();

    /**
     * 循环变量上下文。
     */
    Map<String, Object> getLoopContext();

    /**
     * 获取历史聊天记录。
     */
    List<ChatRecordSimple> getHistoryChatRecords();

    /**
     * 获取历史消息。
     *
     * @param dialogueNumber 对话轮数
     * @param dialogueType   对话类型
     * @param runtimeNodeId  运行时节点 ID
     * @return 历史消息列表
     */
    List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId);

    /**
     * 渲染提示词模板。
     */
    String renderPrompt(String prompt);

    /**
     * 渲染提示词模板（带额外变量）。
     */
    String renderPrompt(String prompt, Map<String, Object> addVariables);

    /**
     * 获取提示词变量。
     */
    Map<String, Object> getPromptVariables();

    /**
     * 获取引用字段值（兼容层，内部委托 {@link NodeReference}；新代码请使用类型化重载）。
     *
     * @param reference 字段引用路径 [nodeId, fieldName]
     * @return 字段值，引用非法时返回 null
     */
    Object getReferenceField(List<String> reference);

    /**
     * 获取引用字段值（类型化引用）。
     *
     * @param reference 类型化节点引用，null 时返回 null
     * @return 字段值
     */
    Object getReferenceField(NodeReference reference);

    /**
     * 获取字段值。
     *
     * @param value  字段值或引用路径
     * @param source 值来源类型
     * @return 实际字段值
     */
    Object getFieldValue(Object value, String source);


    /**
     * 获取开始节点。
     */
    List<AbsNode> startNodes();

    /**
     * 根据节点 ID 获取节点。
     */
    AbsNode getNode(String nodeId);
    /**
     * 获取节点执行超时时间（分钟），与 TimeUnit.MINUTES 配合使用。
     */
    long getNodeExecutionTimeoutMinutes();
}
