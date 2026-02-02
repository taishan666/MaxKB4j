package com.tarzan.maxkb4j.core.workflow.service;

import com.tarzan.maxkb4j.core.workflow.context.WorkflowContext;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;

import java.util.HashMap;
import java.util.Map;

/**
 * 变量解析服务
 * 负责解析工作流中的各种变量：全局变量、聊天变量、节点变量、循环变量
 */
public class VariableResolver {

    private final WorkflowContext context;

    /**
     * 循环变量上下文（用于 LoopWorkFlow）
     */
    private final Map<String, Object> loopContext;

    public VariableResolver(WorkflowContext context) {
        this.context = context;
        this.loopContext = null;
    }

    public VariableResolver(WorkflowContext context, Map<String, Object> loopContext) {
        this.context = context;
        this.loopContext = loopContext;
    }

    /**
     * 获取提示词变量
     * 将所有作用域的变量合并为统一格式，用于模板渲染
     *
     * @return 变量映射，格式为 "scope.variable": value
     */
    public Map<String, Object> getPromptVariables() {
        Map<String, Object> result = new HashMap<>(100);
        // 全局变量: global.xxx
        for (String key : context.getGlobalContext().keySet()) {
            Object value = context.getGlobalContext().get(key);
            result.put("global." + key, value == null ? "*" : value);
        }
        // 聊天变量: chat.xxx
        for (String key : context.getChatContext().keySet()) {
            Object value = context.getChatContext().get(key);
            result.put("chat." + key, value == null ? "*" : value);
        }
        // 循环变量: loop.xxx
        if (loopContext != null) {
            for (String key : loopContext.keySet()) {
                Object value = loopContext.get(key);
                result.put("loop." + key, value == null ? "*" : value);
            }
        }
        for (AbsNode node : context.getNodeContext()) {
            result.putAll(getNodeVariables(node));
        }

        return result;
    }

    /**
     * 获取指定节点的变量
     *
     * @param node 节点对象
     * @return 变量映射，格式为 "nodeName.variable": value
     */
    public Map<String, Object> getNodeVariables(AbsNode node) {
        Map<String, Object> result = new HashMap<>(100);
        String nodeName = node.getProperties().getString("nodeName");
        Map<String, Object> nodeContext = node.getContext();

        for (String key : nodeContext.keySet()) {
            Object value = nodeContext.get(key);
            result.put(nodeName + "." + key, value == null ? "*" : value);
        }

        return result;
    }

    /**
     * 获取所有流变量
     * 用于获取引用字段时查找变量
     *
     * @return 按作用域分组的变量映射
     */
    public Map<String, Map<String, Object>> getFlowVariables() {
        Map<String, Map<String, Object>> result = new HashMap<>(100);
        result.put("global", context.getGlobalContext());
        result.put("chat", context.getChatContext());
        if (loopContext != null) {
            result.put("loop", loopContext);
        }
        for (AbsNode node : context.getNodeContext()) {
            result.put(node.getId(), node.getContext());
        }
        return result;
    }

    /**
     * 获取引用字段的值
     *
     * @param nodeId 节点ID或作用域名称（global, chat, loop）
     * @param key    字段键
     * @return 字段值
     */
    public Object getReferenceField(String nodeId, String key) {
        Map<String, Object> nodeVariable = getFlowVariables().get(nodeId);
        return nodeVariable == null ? null : nodeVariable.get(key);
    }

}
