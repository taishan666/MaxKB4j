package com.tarzan.maxkb4j.module.application.workflow;

import lombok.Data;

import java.util.Map;
import java.util.stream.Stream;

@Data
public class NodeResult {
    private Map<String, Object> nodeVariable;
    private Map<String, Object> workflowVariable;
    private WriteContextFunction writeContextFunc;
    private IsInterruptFunction isInterrupt;

    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable) {
        this.nodeVariable = nodeVariable;
        this.workflowVariable = workflowVariable;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.isInterrupt = this::defaultIsInterrupt;
    }

    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, WriteContextFunction writeContextFunc) {
        this.nodeVariable = nodeVariable;
        this.workflowVariable = workflowVariable;
        this.writeContextFunc = writeContextFunc;
        this.isInterrupt = null;
    }


    public Object writeContext(INode currentNode, WorkflowManage workflowManage) {
        return this.writeContextFunc.apply(nodeVariable, workflowVariable, currentNode, workflowManage);
    }

    public boolean isInterruptExec(INode currentNode) {
        return this.isInterrupt.apply(currentNode);
    }

    public boolean defaultIsInterrupt(INode node) {
        return node.getType().equals("form-node") && !(boolean)node.getContext().get("is_submit");
    }

    public Stream<String> defaultWriteContextFunc(Map<String, Object> stepVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        Stream.Builder<String> steambuilder = Stream.builder();
        if (stepVariable != null) {
            node.context.putAll(stepVariable);
            if (workflow.isResult(node, new NodeResult(stepVariable, globalVariable)) && stepVariable.containsKey("answer")) {
                node.answerText = (String) stepVariable.get("answer");
                steambuilder.add(node.answerText);
            }
        }
        if (globalVariable != null) {
            workflow.getContext().putAll(globalVariable);
        }
        if (node.context.containsKey("start_time")) {
            long runTime = System.currentTimeMillis() - (long)node.context.get("start_time");
            node.context.put("runTime", runTime / 1000F);
        }
        return steambuilder.build();
    }


    @FunctionalInterface
    public interface WriteContextFunction {
        Object apply(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode node, WorkflowManage workflow);
    }

    @FunctionalInterface
    interface IsInterruptFunction {
        boolean apply(INode current_node);
    }

    public boolean isAssertionResult() {
        return this.nodeVariable.containsKey("branch_id");
    }


}