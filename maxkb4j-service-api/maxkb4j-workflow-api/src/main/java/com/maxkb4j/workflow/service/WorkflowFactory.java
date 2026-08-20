package com.maxkb4j.workflow.service;

import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.List;

/**
 * 工作流工厂 SPI（契约层）。
 * <p>
 * 供外部模块（application / knowledge）以及引擎内部（循环节点等）统一构造工作流，
 * 避免依赖 workflow 实现模块的具体引擎类。
 * <p>
 * 通过 {@link WorkflowSpec} 以"规格 + 参数"模式描述构建意图，
 * 新增工作流变体只需扩展 {@link WorkflowSpec.Kind} 与实现方，
 * 而无需在接口上追加业务专属方法。实现位于 workflow 实现模块。
 */
public interface WorkflowFactory {

    /**
     * 按规格构造工作流。
     *
     * @param spec 工作流构建规格（kind 必填项已由类型化静态工厂预置并校验）
     * @return 工作流实例
     * @throws IllegalArgumentException 规格类别无匹配实现（如 LOOP 的父工作流类型不支持）
     */
    IWorkflow create(WorkflowSpec spec);
}
