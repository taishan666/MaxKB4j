package com.tarzan.maxkb4j.core.workflow.builder;

import com.tarzan.maxkb4j.core.workflow.factory.NodeFactory;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;

/**
 * 节点构建器
 * 重构后使用 NodeFactory 的注册表模式，符合开闭原则
 *
 */
public class NodeBuilder {

    /**
     * 节点工厂实例（单例）
     */
    private static final NodeFactory FACTORY = new NodeFactory();

    /**
     * 获取节点实例
     *
     * @param lfNode 前端节点数据
     * @return 节点实例
     * @throws IllegalArgumentException 如果 lfNode 为 null
     * @throws IllegalStateException  如果不支持的节点类型
     */
    public static AbsNode getNode(LfNode lfNode) {
        return FACTORY.createNode(lfNode);
    }

    /**
     * 获取节点工厂实例
     * 用于扩展和自定义节点注册
     *
     * @return 节点工厂实例
     */
    public static NodeFactory getFactory() {
        return FACTORY;
    }
}

