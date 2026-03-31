package com.maxkb4j.workflow.handler.node;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 节点处理器抽象基类
 * 提供生命周期钩子和通用模板方法
 *
 * <p>使用泛型 P 表示节点参数类型，自动处理参数解析</p>
 * <p>提供 execute 模板方法，包含完整的执行流程：</p>
 * <ul>
 *   <li>参数解析 - parseParams()</li>
 *   <li>预处理钩子 - preExecute() / onPreExecute()</li>
 *   <li>核心执行 - doExecute()</li>
 *   <li>后处理钩子 - onPostExecute() / postExecute()</li>
 *   <li>错误处理 - onError() / handleError()</li>
 * </ul>
 *
 * @param <P> 节点参数类型
 */
@Slf4j
public abstract class AbstractNodeHandler<P> implements INodeHandler {

    /**
     * 获取参数类型的 Class 对象
     * 子类必须实现以支持自动参数解析
     *
     * @return 参数类型的 Class
     */
    protected abstract Class<P> getParamsClass();

    /**
     * 核心执行逻辑
     * 子类实现具体的业务逻辑
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param params   解析后的参数
     * @return 执行结果
     * @throws Exception 执行异常
     */
    protected abstract NodeResult doExecute(Workflow workflow, AbsNode node, P params) throws Exception;

    /**
     * 模板方法：完整的执行流程
     * 包含参数解析、预处理、执行、后处理、错误处理
     *
     * <p>注意：此方法为 final，子类应覆盖 doExecute() 而不是此方法</p>
     */
    @Override
    public final NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        long startTime = System.currentTimeMillis();
        P params = null;

        try {
            // 1. 解析参数
            params = parseParams(node);

            // 2. 内部预处理（记录开始时间等）
            onPreExecute(workflow, node, params);

            // 3. 外部预处理钩子（子类可覆盖）
            preExecute(workflow, node);

            // 4. 核心执行
            NodeResult result = doExecute(workflow, node, params);

            // 5. 内部后处理
            onPostExecute(workflow, node, params, result);

            // 6. 外部后处理钩子
            postExecute(workflow, node, result);

            // 7. 记录执行时间
            recordExecutionTime(node, startTime);

            return result;

        } catch (Exception ex) {
            // 错误处理
            onError(workflow, node, ex);
            handleError(workflow, node, params, ex);
            throw ex;
        }
    }

    /**
     * 解析节点参数
     * 自动从 node.getNodeData() 解析为泛型类型 P
     *
     * @param node 节点实例
     * @return 解析后的参数对象，可能为 null
     */
    protected P parseParams(AbsNode node) {
        JSONObject nodeData = node.getNodeData();
        Class<P> paramsClass = getParamsClass();
        if (paramsClass == null) {
            log.warn("Cannot parse params: paramsClass is null for handler {}", this.getClass().getSimpleName());
            return null;
        }
        if (nodeData == null || nodeData.isEmpty()) {
            log.warn("Cannot parse params: nodeData is empty for node {}", node.getId());
            return null;
        }
        return nodeData.toJavaObject(paramsClass);
    }

    /**
     * 内部预处理钩子 - 子类可覆盖
     * 默认实现：记录开始时间和参数
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param params   解析后的参数
     */
    protected void onPreExecute(Workflow workflow, AbsNode node, P params) {
        node.getDetail().put("startTime", System.currentTimeMillis());
        if (params != null) {
            node.getDetail().put("params", params);
        }
    }

    /**
     * 内部后处理钩子 - 子类可覆盖
     * 默认实现：空操作
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param params   解析后的参数
     * @param result   执行结果
     */
    protected void onPostExecute(Workflow workflow, AbsNode node, P params, NodeResult result) {
        // 子类可覆盖添加自定义逻辑
    }

    /**
     * 内部错误处理 - 子类可覆盖
     * 默认实现：记录错误信息到节点详情
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param params   解析后的参数（可能为 null）
     * @param ex       异常信息
     */
    protected void handleError(Workflow workflow, AbsNode node, P params, Exception ex) {
        log.error("Node {} execution failed: {}", node.getType(), ex.getMessage(), ex);
        node.setErrMessage(ex.getMessage());
        node.getDetail().put("error", ex.getMessage());
        node.getDetail().put("errorTime", System.currentTimeMillis());
    }

    /**
     * 记录执行时间
     *
     * @param node      节点实例
     * @param startTime 开始时间（毫秒）
     */
    protected void recordExecutionTime(AbsNode node, long startTime) {
        long endTime = System.currentTimeMillis();
        float runTime = (endTime - startTime) / 1000F;
        node.getDetail().put("runTime", runTime);
        node.getDetail().put("endTime", endTime);
        log.debug("Node {} executed in {} seconds", node.getType(), runTime);
    }

    // ==================== 辅助方法 ====================

    /**
     * 辅助方法：写入节点详情
     *
     * @param node  节点实例
     * @param key   键
     * @param value 值
     */
    protected void putDetail(AbsNode node, String key, Object value) {
        node.getDetail().put(key, value);
    }

    /**
     * 辅助方法：批量写入节点详情
     *
     * @param node   节点实例
     * @param details 详情 Map
     */
    protected void putDetails(AbsNode node, Map<String, Object> details) {
        if (details != null) {
            node.getDetail().putAll(details);
        }
    }

    /**
     * 辅助方法：设置答案文本
     * 同时更新节点的 answerText 和 detail
     *
     * @param node  节点实例
     * @param answer 答案文本
     */
    protected void setAnswer(AbsNode node, String answer) {
        node.setAnswerText(answer);
        node.getDetail().put("answer", answer);
    }

    /**
     * 辅助方法：构建简单结果
     *
     * @param variables 变量 Map
     * @return NodeResult
     */
    protected NodeResult buildResult(Map<String, Object> variables) {
        return new NodeResult(variables);
    }

    /**
     * 辅助方法：构建流式输出结果
     *
     * @param variables 变量 Map
     * @return NodeResult（streamOutput=true）
     */
    protected NodeResult buildStreamResult(Map<String, Object> variables) {
        return new NodeResult(variables, true);
    }

    /**
     * 辅助方法：构建可中断结果
     *
     * @param variables    变量 Map
     * @param streamOutput 是否流式输出
     * @return NodeResult（支持中断判断）
     */
    protected NodeResult buildInterruptibleResult(Map<String, Object> variables, boolean streamOutput) {
        return new NodeResult(variables, streamOutput, this::shouldInterrupt);
    }

    /**
     * 从详情中获取中断标志
     *
     * @param node 节点实例
     * @return 是否中断
     */
    protected boolean getInterruptFlag(AbsNode node) {
        Object flag = node.getDetail().get("is_interrupt_exec");
        return flag != null && Boolean.TRUE.equals(flag);
    }

    /**
     * 设置中断标志
     *
     * @param node     节点实例
     * @param interrupt 是否中断
     */
    protected void setInterruptFlag(AbsNode node, boolean interrupt) {
        node.getDetail().put("is_interrupt_exec", interrupt);
    }

    /**
     * 获取引用字段值
     *
     * @param workflow 工作流上下文
     * @param fields   字段引用路径
     * @return 字段值
     */
    protected Object getReferenceField(Workflow workflow, java.util.List<String> fields) {
        return workflow.getReferenceField(fields);
    }

    /**
     * 获取字符串类型的引用字段值
     *
     * @param workflow 工作流上下文
     * @param fields   字段引用路径
     * @return 字段值（字符串），如果不存在或类型不匹配返回 null
     */
    protected String getReferenceFieldAsString(Workflow workflow, java.util.List<String> fields) {
        Object value = workflow.getReferenceField(fields);
        return value instanceof String ? (String) value : null;
    }
}