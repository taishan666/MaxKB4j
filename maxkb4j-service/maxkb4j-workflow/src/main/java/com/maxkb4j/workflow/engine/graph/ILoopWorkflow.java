package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.LoopParams;

/**
 * 循环工作流契约
 * 循环工作流本身也是完整的工作流（继承 {@link IWorkflow}），
 * 额外提供循环参数（当前迭代索引与元素）访问。
 */
public interface ILoopWorkflow extends IWorkflow {

    /**
     * 获取循环参数（当前迭代索引与元素）
     *
     * @return 循环参数
     */
    LoopParams getLoopParams();

}
