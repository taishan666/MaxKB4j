package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.HashMap;
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

    public NodeResult(WriteContextFunction writeContextFunc) {
        this.nodeVariable = new HashMap<>();
        this.workflowVariable = new HashMap<>();
        this.writeContextFunc = writeContextFunc;
        this.isInterrupt = null;
    }

    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable , INode currentNode, WorkflowManage workflow,WriteContextFunction writeContextFunc) {
        this.nodeVariable = nodeVariable;
        this.workflowVariable = workflowVariable;
        this.writeContextFunc = writeContextFunc;
        this.isInterrupt = null;
    }


    public Map<String, JSONObject> writeContext(INode currentNode, WorkflowManage workflowManage) {
        return this.writeContextFunc.apply(nodeVariable, workflowVariable,currentNode,workflowManage);
    }


    @FunctionalInterface
    public interface WriteContextFunction {
        Map<String, JSONObject>  apply(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode node, WorkflowManage workflow);
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