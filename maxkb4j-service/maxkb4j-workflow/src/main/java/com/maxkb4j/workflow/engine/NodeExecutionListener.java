package com.maxkb4j.workflow.engine;

import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;

/**
 * 节点执行监听器（引擎层回调契约）。
 * <p>
 * 将节点生命周期事件从引擎协作者（{@link NodeChainRunner}、{@link NodeLifecycleExecutor}）
 * 反转为回调，使引擎不依赖工作流处理器层次。类型参数 {@code W} 让实现方（如
 * {@code AbsWorkflowHandler}）直接以目标工作流契约接收事件，无需自行收窄类型。
 * </p>
 *
 * @param <W> 监听方接受的工作流契约
 */
public interface NodeExecutionListener<W extends IWorkflow> {

    /**
     * 节点开始执行：依赖就绪检查通过后、节点处理器调用前触发。
     */
    default void onNodeStart(W workflow, AbsNode node) {
    }

    /**
     * 节点执行成功：结果写入、SUCCESS 状态应用、后继节点解析前触发。
     */
    default void onNodeSuccess(W workflow, AbsNode node, NodeResult result) {
    }

    /**
     * 节点执行失败：调度等待失败或节点执行异常时触发，
     * 实现方负责异常解析与 ERROR 状态标记。
     */
    default void onNodeError(W workflow, AbsNode node, Exception ex) {
    }
}
