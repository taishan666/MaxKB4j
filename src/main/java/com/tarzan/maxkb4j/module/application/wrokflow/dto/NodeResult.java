package com.tarzan.maxkb4j.module.application.wrokflow.dto;

import java.util.Map;

public class NodeResult {
    private final Map<String, Object> nodeVariable;
    private final Map<String, Object> workflowVariable;
    private final WriteContextFunction writeContextFunc;
    private final IsInterruptFunction isInterrupt;

    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> of, WriteContextFunction writeContextFunc) {
        this.nodeVariable = nodeVariable;
        this.workflowVariable = workflowVariable;
        this.writeContextFunc = writeContextFunc;
        this.isInterrupt = null;
    }

    @FunctionalInterface
    public interface WriteContextFunction {
        void apply(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable);
    }

    @FunctionalInterface
    interface IsInterruptFunction {
        boolean apply(Object current_node, Map<String, Object> nodeVariable, Map<String, Object> workflowVariable);
    }


    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable,
                      WriteContextFunction writeContextFunc, IsInterruptFunction isInterrupt) {
        this.nodeVariable = nodeVariable;
        this.workflowVariable = workflowVariable;
        this.writeContextFunc = writeContextFunc;
        this.isInterrupt = isInterrupt;
    }

}