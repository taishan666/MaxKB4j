package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.node.AbsNode;

import java.util.List;
import java.util.Map;

/**
 * 工作流上下文访问器接口（契约层）。
 * <p>
 * 定义工作流各级上下文（全局/聊天/节点/循环）的访问与变更行为，供 {@link IWorkflow} 门面返回。
 * 具体实现位于 workflow 实现模块。
 */
public interface IWorkflowContext {

    /**
     * 添加或更新节点到上下文（按 id + runtimeNodeId 去重替换）。
     */
    void appendNode(AbsNode currentNode);

    /**
     * 渲染模板（使用上下文全部变量）。
     */
    String render(String prompt);

    /**
     * 渲染模板（合并额外变量，覆盖同名上下文变量；addVariables 为 null 时不合并）。
     */
    String render(String prompt, Map<String, Object> addVariables);

    /**
     * 获取提示词变量（"scope.variable": value 统一格式）。
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
     * 获取引用字段值（nodeId 为节点 ID 或作用域名 global/chat/loop）。
     */
    Object getReferenceField(String nodeId, String key);
    /**
     * 获取字段值。
     *
     * @param value  字段值或引用路径
     * @param source 值来源类型
     * @return 实际字段值
     */
    Object getFieldValue(Object value, String source);
    /**
     * 获取已执行节点（按 nodeId）。
     */
    AbsNode getExecutedNode(String nodeId);

    /**
     * 全局变量上下文。
     */
    Map<String, Object> getGlobalContext();

    /**
     * 聊天变量上下文。
     */
    Map<String, Object> getChatContext();

    /**
     * 节点变量上下文列表。
     */
    List<AbsNode> getNodeContext();

    /**
     * 循环变量上下文。
     */
    Map<String, Object> getLoopContext();
}
