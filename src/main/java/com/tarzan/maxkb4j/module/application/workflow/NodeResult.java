package com.tarzan.maxkb4j.module.application.workflow;

import lombok.Data;

import java.util.Map;

@Data
public class NodeResult {
    private  Map<String, Object> nodeVariable;
    private  Map<String, Object> workflowVariable;
    private  WriteContextFunction writeContextFunc;
    private  IsInterruptFunction isInterrupt;

    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, WriteContextFunction writeContextFunc) {
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

    public boolean isAssertionResult(){
        return this.nodeVariable.containsKey("branch_id");
    }


}