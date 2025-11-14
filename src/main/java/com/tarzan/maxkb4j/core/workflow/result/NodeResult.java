package com.tarzan.maxkb4j.core.workflow.result;

import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

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

    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, IsInterruptFunction isInterrupt) {
        this.nodeVariable = nodeVariable;
        this.globalVariable = globalVariable;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.isInterrupt = isInterrupt;
    }
    public NodeResult(Map<String, Object> nodeVariable, Map<String, Object> globalVariable , WriteContextFunction writeContextFunc,IsInterruptFunction isInterrupt) {
        this.nodeVariable = nodeVariable;
        this.globalVariable = globalVariable;
        this.writeContextFunc = writeContextFunc;
        this.isInterrupt = isInterrupt;
    }



    public void writeContext(INode currentNode, Workflow workflow) {
        this.writeContextFunc.apply(nodeVariable, globalVariable, currentNode, workflow);
    }

    public boolean isInterruptExec(INode currentNode) {
        return this.isInterrupt.apply(currentNode);
    }

    public boolean defaultIsInterrupt(INode node) {
        return false;
    }

    public void defaultWriteContextFunc(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, Workflow workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            node.getDetail().putAll(nodeVariable);
            if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable))&& StringUtil.isNotBlank(node.getAnswerText())) {
                ChatMessageVO vo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        node.getAnswerText(),
                        "",
                        false);
                workflow.getChatParams().getSink().tryEmitNext(vo);
                workflow.setAnswer(workflow.getAnswer()+node.getAnswerText());
                ChatMessageVO endVo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        "",
                        "",
                        true);
                workflow.getChatParams().getSink().tryEmitNext(endVo);
            }
        }
        if (globalVariable != null) {
            workflow.getContext().putAll(globalVariable);
            node.getDetail().putAll(globalVariable);
        }
    }


    @FunctionalInterface
    public interface WriteContextFunction {
        void apply(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode node, Workflow workflow);
    }

    @FunctionalInterface
    public interface IsInterruptFunction {
        boolean apply(INode currentNode);
    }

    public boolean isAssertionResult() {
        return this.nodeVariable.containsKey("branchId");
    }


}