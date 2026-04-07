package com.maxkb4j.workflow.builder;

import com.maxkb4j.workflow.logic.LfNode;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.service.INodeCreator;

/**
 * 节点构建器
 * 重构后为非静态类，通过依赖注入使用
 *
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