package com.tarzan.maxkb4j.core.workflow.factory;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点注册表
 * 管理节点类型与节点创建函数的映射关系
 *
 * 线程安全设计：
 * - 使用 ConcurrentHashMap 存储注册表
 * - 支持运行时动态注册
 */
@Slf4j
public class NodeRegistry {

    /**
     * 节点创建函数映射
     * Key: 节点类型标识
     * Value: 节点创建函数
     */
    private final Map<String, NodeFactory.NodeCreator> creators;

    public NodeRegistry() {
        this.creators = new ConcurrentHashMap<>();
    }

    /**
     * 注册节点创建函数
     *
     * @param nodeType 节点类型标识
     * @param creator  节点创建函数
     * @throws IllegalArgumentException 如果 nodeType 或 creator 为 null
     */
    public void register(String nodeType, NodeFactory.NodeCreator creator) {
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("节点类型不能为空");
        }
        if (creator == null) {
            throw new IllegalArgumentException("节点创建函数不能为 null");
        }

        creators.put(nodeType, creator);
        log.debug("注册节点类型: {}", nodeType);
    }

    /**
     * 获取节点创建函数
     *
     * @param nodeType 节点类型标识
     * @return 节点创建函数，如果不存在返回 null
     */
    public NodeFactory.NodeCreator getCreator(String nodeType) {
        if (nodeType == null) {
            return null;
        }
        return creators.get(nodeType);
    }

    /**
     * 检查是否已注册指定节点类型
     *
     * @param nodeType 节点类型标识
     * @return 是否已注册
     */
    public boolean isRegistered(String nodeType) {
        return creators.containsKey(nodeType);
    }

    /**
     * 获取已注册的节点类型数量
     *
     * @return 节点类型数量
     */
    public int size() {
        return creators.size();
    }

    /**
     * 清空注册表
     * 主要用于测试场景
     */
    public void clear() {
        creators.clear();
        log.debug("清空节点注册表");
    }

    /**
     * 注销节点类型
     *
     * @param nodeType 节点类型标识
     * @return 被移除的创建函数，如果不存在返回 null
     */
    public NodeFactory.NodeCreator unregister(String nodeType) {
        NodeFactory.NodeCreator removed = creators.remove(nodeType);
        if (removed != null) {
            log.debug("注销节点类型: {}", nodeType);
        }
        return removed;
    }
}
