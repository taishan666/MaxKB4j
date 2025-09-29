package com.tarzan.maxkb4j.core.workflow.result;

import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

@Slf4j
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
            node.getContext().putAll(nodeVariable);
            if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable)) && nodeVariable.containsKey("answer")) {
                String answer = (String) nodeVariable.get("answer");
                ChatMessageVO vo = new ChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        node.getId(),
                        answer,
                        "",
                        node.getRuntimeNodeId(),
                        node.getType(),
                        node.getViewType(),
                        false);
                node.getChatParams().getSink().tryEmitNext(vo);
                workflow.setAnswer(answer);
            }
        }
        if (globalVariable != null) {
            workflow.getGlobalVariable().putAll(globalVariable);
        }
        ChatMessageVO vo = new ChatMessageVO(
                workflow.getChatParams().getChatId(),
                workflow.getChatParams().getChatRecordId(),
                node.getId(),
                "\n",
                "",
                node.getRuntimeNodeId(),
                node.getType(),
                node.getViewType(),
                true);
        node.getChatParams().getSink().tryEmitNext(vo);
        log.info("WriteContext node: {} ",node.getType());
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
        return this.nodeVariable.containsKey("branchId");
    }


}