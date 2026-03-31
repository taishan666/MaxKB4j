package com.maxkb4j.workflow.registry;

import com.maxkb4j.workflow.handler.node.INodeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点处理器注册表
 * 管理节点类型与处理器实例的映射关系
 *
 * 从 NodeCenter 分离，遵循单一职责原则
 */
@Slf4j
@Component
public class NodeHandlerRegistry {

    /**
     * 处理器注册表
     * Key: 节点类型标识 (如 "ai-chat-node")
     * Value: 处理器实例
     */
    private final Map<String, INodeHandler> handlers;

    public NodeHandlerRegistry() {
        this.handlers = new ConcurrentHashMap<>();
    }

    /**
     * 注册处理器
     *
     * @param nodeType 节点类型标识
     * @param handler  处理器实例
     * @return true 如果该类型已有处理器被覆盖
     * @throws IllegalArgumentException 如果 nodeType 或 handler 为 null
     */
    public boolean register(String nodeType, INodeHandler handler) {
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("节点类型不能为空");
        }
        if (handler == null) {
            throw new IllegalArgumentException("处理器不能为 null");
        }
        boolean replaced = handlers.put(nodeType, handler) != null;
        if (replaced) {
            log.warn("Node handler for type '{}' was replaced by {}", nodeType, handler.getClass().getSimpleName());
        } else {
            log.debug("Registered node handler: {} -> {}", nodeType, handler.getClass().getSimpleName());
        }
        return replaced;
    }

    /**
     * 获取处理器
     *
     * @param nodeType 节点类型标识
     * @return 处理器实例
     * @throws IllegalStateException 如果处理器不存在
     */
    public INodeHandler get(String nodeType) {
        INodeHandler handler = handlers.get(nodeType);
        if (handler == null) {
            throw new IllegalStateException("No handler found for node type: " + nodeType);
        }
        return handler;
    }

    /**
     * 检查是否已注册指定节点类型的处理器
     *
     * @param nodeType 节点类型标识
     * @return 是否已注册
     */
    public boolean has(String nodeType) {
        return handlers.containsKey(nodeType);
    }

    /**
     * 获取已注册的处理器数量
     *
     * @return 处理器数量
     */
    public int size() {
        return handlers.size();
    }

    /**
     * 获取所有已注册的节点类型
     *
     * @return 节点类型集合
     */
    public java.util.Set<String> getRegisteredTypes() {
        return java.util.Collections.unmodifiableSet(handlers.keySet());
    }
}