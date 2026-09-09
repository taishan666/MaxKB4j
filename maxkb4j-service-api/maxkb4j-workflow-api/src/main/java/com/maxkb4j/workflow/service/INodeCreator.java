package com.maxkb4j.workflow.service;

import com.maxkb4j.workflow.logic.LfNode;
import com.maxkb4j.workflow.node.INode;

/**
 * 节点创建器接口
 * 定义节点创建的标准方法
 */
public interface INodeCreator {

    /**
     * 创建节点实例
     *
     * @param lfNode 前端节点数据
     * @return 节点实例
     */
    INode createNode(LfNode lfNode);

}