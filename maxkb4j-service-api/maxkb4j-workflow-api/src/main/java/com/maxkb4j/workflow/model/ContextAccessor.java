package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.node.AbsNode;

import java.util.List;
import java.util.Map;

/**
 * 上下文访问器
 * 提供类型安全的上下文访问方法，封装 WorkflowContext 的直接访问
 *
 * 设计原则：
 * - 提供清晰的语义化方法名
 * - 避免暴露内部 Map 的直接操作
 * - 统一访问入口
 */
public class ContextAccessor {

    private final WorkflowContext context;

    ContextAccessor(WorkflowContext context) {
        this.context = context;
    }

    // ==================== 读取方法 ====================

    /**
     * 获取全局上下文
     *
     * @return 全局变量 Map
     */
    public Map<String, Object> global() {
        return context.getGlobalContext();
    }

    /**
     * 获取聊天上下文
     *
     * @return 聊天变量 Map
     */
    public Map<String, Object> chat() {
        return context.getChatContext();
    }

    /**
     * 获取节点上下文列表
     *
     * @return 已执行的节点列表
     */
    public List<AbsNode> nodes() {
        return context.getNodeContext();
    }

    /**
     * 获取全局变量
     *
     * @param key 变量名
     * @return 变量值，不存在返回 null
     */
    public Object getGlobal(String key) {
        return context.getGlobalContext().get(key);
    }

    /**
     * 获取聊天变量
     *
     * @param key 变量名
     * @return 变量值，不存在返回 null
     */
    public Object getChat(String key) {
        return context.getChatContext().get(key);
    }

    /**
     * 获取全局字符串变量
     *
     * @param key 变量名
     * @return 字符串值，不存在或类型不匹配返回 null
     */
    public String getGlobalAsString(String key) {
        Object value = getGlobal(key);
        return value instanceof String ? (String) value : null;
    }

    /**
     * 获取聊天字符串变量
     *
     * @param key 变量名
     * @return 字符串值，不存在或类型不匹配返回 null
     */
    public String getChatAsString(String key) {
        Object value = getChat(key);
        return value instanceof String ? (String) value : null;
    }

    // ==================== 写入方法 ====================

    /**
     * 设置全局变量
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void setGlobal(String key, Object value) {
        context.getGlobalContext().put(key, value);
    }

    /**
     * 设置聊天变量
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void setChat(String key, Object value) {
        context.getChatContext().put(key, value);
    }

    /**
     * 批量设置全局变量
     *
     * @param variables 变量 Map
     */
    public void setGlobalAll(Map<String, Object> variables) {
        if (variables != null) {
            context.getGlobalContext().putAll(variables);
        }
    }

    /**
     * 批量设置聊天变量
     *
     * @param variables 变量 Map
     */
    public void setChatAll(Map<String, Object> variables) {
        if (variables != null) {
            context.getChatContext().putAll(variables);
        }
    }

    /**
     * 添加节点到上下文
     *
     * @param node 节点实例
     */
    public void appendNode(AbsNode node) {
        context.appendNode(node);
    }

    // ==================== 检查方法 ====================

    /**
     * 检查全局变量是否存在
     *
     * @param key 变量名
     * @return 是否存在
     */
    public boolean hasGlobal(String key) {
        return context.getGlobalContext().containsKey(key);
    }

    /**
     * 检查聊天变量是否存在
     *
     * @param key 变量名
     * @return 是否存在
     */
    public boolean hasChat(String key) {
        return context.getChatContext().containsKey(key);
    }

    /**
     * 移除全局变量
     *
     * @param key 变量名
     * @return 被移除的值
     */
    public Object removeGlobal(String key) {
        return context.getGlobalContext().remove(key);
    }

    /**
     * 移除聊天变量
     *
     * @param key 变量名
     * @return 被移除的值
     */
    public Object removeChat(String key) {
        return context.getChatContext().remove(key);
    }

    /**
     * 清空全局上下文
     */
    public void clearGlobal() {
        context.getGlobalContext().clear();
    }

    /**
     * 清空聊天上下文
     */
    public void clearChat() {
        context.getChatContext().clear();
    }

    /**
     * 获取全局上下文大小
     *
     * @return 变量数量
     */
    public int globalSize() {
        return context.getGlobalContext().size();
    }

    /**
     * 获取聊天上下文大小
     *
     * @return 变量数量
     */
    public int chatSize() {
        return context.getChatContext().size();
    }

    /**
     * 获取节点上下文大小
     *
     * @return 节点数量
     */
    public int nodeSize() {
        return context.getNodeContext().size();
    }
}