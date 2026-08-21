package com.maxkb4j.workflow.builder;

import com.maxkb4j.workflow.logic.LfNode;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.service.INodeCreator;

/**
 * 节点构建器（实现层）
 * <p>
 * 对 {@link INodeCreator} 契约的便捷包装，属于实现细节，仅供 workflow 实现模块内部使用；
 * 外部模块请直接注入 {@link INodeCreator} 契约接口。
 * <p>
 * 使用方式：
 * - 通过 Spring 注入 NodeBuilder Bean
 * - 调用 nodeBuilder.getNode(lfNode) 创建节点实例
 */
public class NodeBuilder {

    private final INodeCreator nodeCreator;

    /**
     * 构造器，接收节点创建器
     *
     * @param nodeCreator 节点创建器实现（由实现层提供）
     */
    public NodeBuilder(INodeCreator nodeCreator) {
        this.nodeCreator = nodeCreator;
    }

    /**
     * 获取节点实例
     *
     * @param lfNode 前端节点数据
     * @return 节点实例
     */
    public AbsNode getNode(LfNode lfNode) {
        return nodeCreator.createNode(lfNode);
    }

}
