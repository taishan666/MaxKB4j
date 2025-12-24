package com.tarzan.maxkb4j.core.workflow.builder;


import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeHandlerBuilder {

    private static final Map<String, INodeHandler> HANDLER_POOL = new ConcurrentHashMap<>();

    /**
     * 动态注册处理器（供自动注册器使用）
     *
     * @return true 如果该类型已有处理器被覆盖
     */
    public static boolean registerHandler(NodeType nodeType, INodeHandler handler) {
        if (nodeType == null || handler == null) {
            throw new IllegalArgumentException("nodeType and handler must not be null");
        }
        return HANDLER_POOL.put(nodeType.getKey(), handler) != null;
    }

    public static INodeHandler getHandler(String nodeType) {
        INodeHandler handler = HANDLER_POOL.get(nodeType);
        if (handler == null) {
            throw new ApiException("No handler found for node type: " + nodeType);
        }
        return handler;
    }

    public static boolean supports(String nodeType) {
        return HANDLER_POOL.containsKey(nodeType);
    }

}
