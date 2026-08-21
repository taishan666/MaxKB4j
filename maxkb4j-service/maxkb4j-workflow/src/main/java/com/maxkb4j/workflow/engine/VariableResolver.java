package com.maxkb4j.workflow.engine;


import com.maxkb4j.workflow.node.AbsNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

/**
 * Variable resolver service
 * Responsible for resolving various variables in workflows: global variables, chat variables, node variables, loop variables
 */
public class VariableResolver {

    private final WorkflowContext context;


    public VariableResolver(WorkflowContext context) {
        this.context = context;
    }

    /**
     * Get prompt variables
     * Merge all scope variables into unified format for template rendering
     *
     * @return variable map in "scope.variable": value format
     */
    public Map<String, Object> getPromptVariables() {
        Map<String, Object> result = getStringObjectMap();
        // Global variables: global.xxx
        if (context.getGlobalContext() != null) {
            for (Map.Entry<String, Object> entry : context.getGlobalContext().entrySet()) {
                result.put(Scope.GLOBAL_PREFIX + entry.getKey(), entry.getValue());
            }
        }

        // Chat variables: chat.xxx
        if (context.getChatContext() != null) {
            for (Map.Entry<String, Object> entry : context.getChatContext().entrySet()) {
                result.put(Scope.CHAT_PREFIX + entry.getKey(), entry.getValue());
            }
        }

        // Loop variables: loop.xxx
        if (context.getLoopContext()  != null) {
            for (Map.Entry<String, Object> entry : context.getLoopContext().entrySet()) {
                result.put(Scope.LOOP_PREFIX + entry.getKey(), entry.getValue());
            }
        }
        // Node variables
        if (context.getNodeContext() != null) {
            for (AbsNode node : context.getNodeContext()) {
                result.putAll(getNodeVariables(node));
            }
        }

        return result;
    }

    private @NotNull Map<String, Object> getStringObjectMap() {
        int globalSize = context.getGlobalContext() != null ? context.getGlobalContext().size() : 0;
        int chatSize = context.getChatContext() != null ? context.getChatContext().size() : 0;
        int loopSize = context.getLoopContext() != null ? context.getLoopContext().size() : 0;
        int nodeCount = context.getNodeContext() != null ? context.getNodeContext().size() : 0;
        // Estimate: each node has ~3 variables on average
        int estimatedSize = globalSize + chatSize + loopSize + (nodeCount * 3) + 16;
        return new HashMap<>(estimatedSize);
    }

    /**
     * Get variables for a specific node
     *
     * @param node node object
     * @return variable map in "nodeName.variable": value format
     */
    public Map<String, Object> getNodeVariables(AbsNode node) {
        if (node == null || node.getProperties() == null) {
            return new HashMap<>(0);
        }
        String nodeName = node.getProperties().getString(RuntimeDetailField.NODE_NAME);
        Map<String, Object> nodeContext = node.getContext();

        if (nodeName == null || nodeContext == null) {
            return new HashMap<>(0);
        }

        Map<String, Object> result = new HashMap<>(nodeContext.size() + 4);

        for (Map.Entry<String, Object> entry : nodeContext.entrySet()) {
            Object value = entry.getValue();
            result.put(nodeName + "." + entry.getKey(), value == null ? "*" : value);
        }

        return result;
    }

    /**
     * Get all flow variables
     * Used for finding variables when getting reference fields
     *
     * @return variable map grouped by scope
     */
    public Map<String, Map<String, Object>> getFlowVariables() {
        // Estimate capacity: global + chat + loop + nodes
        int nodeCount = context.getNodeContext() != null ? context.getNodeContext().size() : 0;
        int estimatedSize = 3 + nodeCount + 4;
        Map<String, Map<String, Object>> result = new HashMap<>(estimatedSize);
        result.put(Scope.GLOBAL, context.getGlobalContext() != null ? context.getGlobalContext() : new HashMap<>());
        result.put(Scope.CHAT, context.getChatContext() != null ? context.getChatContext() : new HashMap<>());
        result.put(Scope.LOOP, context.getLoopContext() != null ? context.getLoopContext() : new HashMap<>());

        if (context.getNodeContext() != null) {
            for (AbsNode node : context.getNodeContext()) {
                if (node != null && node.getId() != null) {
                    result.put(node.getId(), node.getContext() != null ? node.getContext() : new HashMap<>());
                }
            }
        }

        return result;
    }

    /**
     * Get reference field value
     *
     * @param nodeId node ID or scope name (global, chat, loop)
     * @param key    field key
     * @return field value
     */
    public Object getReferenceField(String nodeId, String key) {
        if (nodeId == null || key == null) {
            return null;
        }
        Map<String, Map<String, Object>> flowVariables = getFlowVariables();
        Map<String, Object> nodeVariable = flowVariables.get(nodeId);
        return nodeVariable == null ? null : nodeVariable.get(key);
    }

}
