package com.maxkb4j.workflow.handler.node;


import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;

/**
 * 节点处理器接口
 * 支持生命周期钩子扩展
 */
public interface INodeHandler {

    /**
     * 执行节点处理
     * 核心执行方法，必须实现
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @return 执行结果
     * @throws Exception 执行异常
     */
    NodeResult execute(Workflow workflow, AbsNode node) throws Exception;

    /**
     * 预处理钩子 - 执行前调用
     * 可用于：参数校验、状态初始化、权限检查等
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     */
    default void preExecute(Workflow workflow, AbsNode node) {
        // 默认空实现
    }

    /**
     * 后处理钩子 - 执行成功后调用
     * 可用于：结果后处理、状态更新、日志记录等
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param result   执行结果
     */
    default void postExecute(Workflow workflow, AbsNode node, NodeResult result) {
        // 默认空实现
    }

    /**
     * 错误处理钩子 - 执行异常时调用
     * 可用于：错误日志、错误恢复、通知发送等
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param ex       异常信息
     */
    default void onError(Workflow workflow, AbsNode node, Exception ex) {
        // 默认空实现
    }

    /**
     * 判断是否需要中断工作流执行
     *
     * @param node 节点实例
     * @return 是否中断
     */
    default boolean shouldInterrupt(AbsNode node) {
        return false;
    }

}
