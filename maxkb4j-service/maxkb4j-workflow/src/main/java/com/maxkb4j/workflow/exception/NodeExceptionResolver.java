package com.maxkb4j.workflow.exception;

import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;

/**
 * 节点异常解析器接口
 * 采用责任链模式，允许多个处理器按顺序处理异常
 *
 * @see com.maxkb4j.workflow.exception.impl.LoggingExceptionResolver
 * @see com.maxkb4j.workflow.exception.impl.DetailRecordingResolver
 */
public interface NodeExceptionResolver {

    /**
     * 解析/处理节点执行异常
     *
     * @param workflow 工作流上下文
     * @param node     发生异常的节点
     * @param ex       异常信息
     * @return true 继续执行下一个解析器，false 终止责任链
     */
    boolean resolve(Workflow workflow, AbsNode node, Exception ex);

    /**
     * 获取解析器顺序
     * 数值越小优先级越高
     *
     * @return 顺序值
     */
    default int getOrder() {
        return 100;
    }
}