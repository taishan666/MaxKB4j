package com.tarzan.maxkb4j.core.workflow;

import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import lombok.Data;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

@Data
public class NodeResult {
    private Map<String, Object> nodeVariable;
    private Map<String, Object> globalVariable;
    private WriteContextFunction writeContextFunc;
    private IsInterruptFunction isInterrupt;

    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> globalVariable) {
        this.nodeVariable = nodeVariable;
        this.globalVariable = globalVariable;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.isInterrupt = this::defaultIsInterrupt;
    }

    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, WriteContextFunction writeContextFunc) {
        this.nodeVariable = nodeVariable;
        this.globalVariable = globalVariable;
        this.writeContextFunc = writeContextFunc;
        this.isInterrupt = this::defaultIsInterrupt;
    }


    public void writeContext(INode currentNode, WorkflowManage workflowManage) {
        this.writeContextFunc.apply(nodeVariable, globalVariable, currentNode, workflowManage);
    }

    public boolean isInterruptExec(INode currentNode) {
        return this.isInterrupt.apply(currentNode);
    }

    public boolean defaultIsInterrupt(INode node) {
        return (USER_SELECT.getKey().equals(node.getType())|| FORM.getKey().equals(node.getType())) && !(boolean)node.getContext().get("is_submit");
    }

    public void defaultWriteContextFunc(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        if (nodeVariable != null) {
            node.context.putAll(nodeVariable);
            if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable)) && nodeVariable.containsKey("answer")) {
                String answer = (String) nodeVariable.get("answer");
                ChatMessageVO vo = new ChatMessageVO(
                        workflow.getFlowParams().getChatId(),
                        workflow.getFlowParams().getChatRecordId(),
                        answer,
                        node.runtimeNodeId,
                        node.type,
                        "many_view",
                        true,
                        false);
                node.emitter.send(vo);
                workflow.setAnswer(answer);
            }
        }
        if (globalVariable != null) {
            workflow.getContext().putAll(globalVariable);
        }
        if (node.context.containsKey("start_time")) {
            long runTime = System.currentTimeMillis() - (long)node.context.get("start_time");
            node.context.put("runTime", runTime / 1000F);
        }
    }


    @FunctionalInterface
    public interface WriteContextFunction {
        void apply(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode node, WorkflowManage workflow);
    }

    @FunctionalInterface
    interface IsInterruptFunction {
        boolean apply(INode current_node);
    }

    public boolean isAssertionResult() {
        return this.nodeVariable.containsKey("branch_id");
    }


}