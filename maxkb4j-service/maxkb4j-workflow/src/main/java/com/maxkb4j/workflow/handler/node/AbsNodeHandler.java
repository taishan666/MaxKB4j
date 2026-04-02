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
 */
@Slf4j
public abstract class AbsNodeHandler implements INodeHandler {

    /**
     * 核心执行逻辑
     * 子类实现具体的业务逻辑
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @return 执行结果
     * @throws Exception 执行异常
     */
    protected abstract NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception;

    /**
     * 模板方法：完整的执行流程
     * 包含参数解析、预处理、执行、后处理、错误处理
     *
     * <p>注意：此方法为 final，子类应覆盖 doExecute() 而不是此方法</p>
     */
    @Override
    public final NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            // 2. 内部预处理（记录开始时间等）
            onPreExecute(workflow, node);

            // 3. 外部预处理钩子（子类可覆盖）
            preExecute(workflow, node);

            // 4. 核心执行
            NodeResult result = doExecute(workflow, node);

            // 5. 内部后处理
            onPostExecute(workflow, node, result);

            // 6. 外部后处理钩子
            postExecute(workflow, node, result);

            // 7. 记录执行时间
            recordExecutionTime(node, startTime);

            return result;

        } catch (Exception ex) {
            // 错误处理
            onError(workflow, node, ex);
            handleError(workflow, node, ex);
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
    protected <P> P parseParams(AbsNode node,Class<P> paramsClass) {
        JSONObject nodeData = node.getNodeData();
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
     */
    protected void onPreExecute(Workflow workflow, AbsNode node) {
        node.getDetail().put("startTime", System.currentTimeMillis());
    }

    /**
     * 内部后处理钩子 - 子类可覆盖
     * 默认实现：空操作
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param result   执行结果
     */
    protected void onPostExecute(Workflow workflow, AbsNode node, NodeResult result) {
        // 子类可覆盖添加自定义逻辑
    }

    /**
     * 内部错误处理 - 子类可覆盖
     * 默认实现：空操作
     *
     * Note: 所有错误处理（日志记录、详情记录、Sink发送）
     * 已统一由 ExceptionResolverChain 责任链处理。
     * 子类可覆盖此方法添加自定义错误处理逻辑。
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param ex       异常信息
     */
    protected void handleError(Workflow workflow, AbsNode node, Exception ex) {
        // 默认空实现，异常处理由 ExceptionResolverChain 统一处理
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
        String nodeName = node.getProperties() != null ? node.getProperties().getString("nodeName") : node.getType();
        log.info("node: {}, runTime: {} s", nodeName, runTime);
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
     * 从详情中获取中断标志
     *
     * @param node 节点实例
     * @return 是否中断
     */
    protected boolean getInterruptFlag(AbsNode node) {
        Object flag = node.getDetail().get("is_interrupt_exec");
        return Boolean.TRUE.equals(flag);
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