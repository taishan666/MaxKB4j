package com.tarzan.maxkb4j.core.workflow.model;

import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Slf4j
@Data
public class NodeResult {
    private Map<String, Object> nodeVariable;
    private boolean streamOutput;
    private WriteContextFunction writeContextFunc;
    private WriteDetailFunction writeDetailFunc;
    private IsInterruptFunction isInterrupt;


    public NodeResult(Map<String, Object> nodeVariable) {
        this.nodeVariable = nodeVariable;
        this.streamOutput = false;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.writeDetailFunc = this::defaultWriteDetailFunc;
        this.isInterrupt = this::defaultIsInterrupt;
    }

    public NodeResult(Map<String, Object> nodeVariable, boolean streamOutput) {
        this.nodeVariable = nodeVariable;
        this.streamOutput = streamOutput;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.writeDetailFunc = this::defaultWriteDetailFunc;
        this.isInterrupt = this::defaultIsInterrupt;
    }


    public NodeResult(Map<String, Object> nodeVariable,  boolean streamOutput, IsInterruptFunction isInterrupt) {
        this.nodeVariable = nodeVariable;
        this.streamOutput = streamOutput;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.writeDetailFunc = this::defaultWriteDetailFunc;
        this.isInterrupt = isInterrupt;
    }


    public void writeContext(INode currentNode, Workflow workflow) {
        this.writeContextFunc.apply(nodeVariable, currentNode, workflow);
    }

    public void writeDetail(INode currentNode) {
        this.writeDetailFunc.apply(nodeVariable, currentNode);
    }

    public boolean isInterruptExec(INode currentNode) {
        return this.isInterrupt.apply(currentNode);
    }

    public boolean defaultIsInterrupt(INode node) {
        return false;
    }

    public void defaultWriteContextFunc(Map<String, Object> nodeVariable, INode node, Workflow workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            if (StringUtils.isNotBlank(node.getAnswerText())) {
                if (!this.streamOutput) {
                    ChatMessageVO vo = node.toChatMessageVO(
                            workflow.getChatParams().getChatId(),
                            workflow.getChatParams().getChatRecordId(),
                            node.getAnswerText(),
                            "",
                            false);
                    workflow.getSink().tryEmitNext(vo);
                }
                workflow.setAnswer(workflow.getAnswer() + node.getAnswerText());
                ChatMessageVO endVo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        "",
                        "",
                        true);
                workflow.getSink().tryEmitNext(endVo);
            }
        }
    }

    public void defaultWriteDetailFunc(Map<String, Object> nodeVariable, INode node) {
        if (nodeVariable != null) {
            if (NodeType.FORM.getKey().equals(node.getType())) {
                node.getDetail().put("form_data", nodeVariable.get("form_data"));
                node.getDetail().put("is_submit", nodeVariable.get("is_submit"));
            } else {
                node.getDetail().putAll(nodeVariable);
            }
        }
    }


    @FunctionalInterface
    public interface WriteContextFunction {
        void apply(Map<String, Object> nodeVariable, INode node, Workflow workflow);
    }


    @FunctionalInterface
    public interface WriteDetailFunction {
        void apply(Map<String, Object> nodeVariable, INode node);
    }

    @FunctionalInterface
    public interface IsInterruptFunction {
        boolean apply(INode currentNode);
    }

    public boolean isAssertionResult() {
        return this.nodeVariable.containsKey("branchId");
    }


}