package com.maxkb4j.workflow.registry;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.logic.LfNode;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.service.INodeCreator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified node center: the single registry for both node creators and node handlers.
 *
 * <p>Merges the former trio ({@code NodeRegistry}, {@code NodeHandlerRegistry} and the
 * {@code NodeCenter} facade) into one component. Registration is now annotation-driven:
 * <ul>
 *   <li>{@link com.maxkb4j.workflow.annotation.NodeCreatorType} &rarr;
 *       {@link com.maxkb4j.workflow.processor.NodeCreatorAutoRegistrar} registers creators.</li>
 *   <li>{@link com.maxkb4j.workflow.annotation.NodeHandlerType} &rarr;
 *       {@link com.maxkb4j.workflow.processor.NodeHandlerAutoRegistrar} registers handlers.</li>
 * </ul>
 * This removes the hand-written register table and the static lookups.
 */
@Slf4j
@Component
public class NodeCenter implements INodeCreator {

    /** Functional contract for instantiating a node from its id and properties. */
    @FunctionalInterface
    public interface NodeCreator {
        Object create(String id, JSONObject properties);
    }

    private final Map<String, NodeCreator> creators = new ConcurrentHashMap<>();
    private final Map<String, INodeHandler> handlers = new ConcurrentHashMap<>();

    // ---- node creators -----------------------------------------------------

    public void registerCreator(String nodeType, NodeCreator creator) {
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("Node type must not be blank");
        }
        if (creator == null) {
            throw new IllegalArgumentException("Node creator must not be null");
        }
        creators.put(nodeType, creator);
        log.debug("Registered node creator: {}", nodeType);
    }

    public NodeCreator getCreator(String nodeType) {
        if (nodeType == null) {
            return null;
        }
        return creators.get(nodeType);
    }

    public int creatorCount() {
        return creators.size();
    }

    // ---- node handlers -----------------------------------------------------

    public boolean registerHandler(String nodeType, INodeHandler handler) {
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("Node type must not be blank");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Node handler must not be null");
        }
        boolean replaced = handlers.put(nodeType, handler) != null;
        if (replaced) {
            log.warn("Node handler for type '{}' was replaced by {}", nodeType, handler.getClass().getSimpleName());
        } else {
            log.debug("Registered node handler: {} -> {}", nodeType, handler.getClass().getSimpleName());
        }
        return replaced;
    }

    public INodeHandler getHandler(String nodeType) {
        INodeHandler handler = handlers.get(nodeType);
        if (handler == null) {
            throw new IllegalStateException("No handler found for node type: " + nodeType);
        }
        return handler;
    }

    public boolean hasHandler(String nodeType) {
        return handlers.containsKey(nodeType);
    }

    public int handlerCount() {
        return handlers.size();
    }

    // ---- INodeCreator ------------------------------------------------------

    @Override
    public AbsNode createNode(LfNode lfNode) {
        if (lfNode == null) {
            log.error("LfNode must not be null");
            return null;
        }
        String type = lfNode.getType();
        NodeCreator creator = creators.get(type);
        if (creator == null) {
            log.error("Unsupported node type: {}", type);
            return null;
        }
        return (AbsNode) creator.create(lfNode.getId(), lfNode.getProperties());
    }
}