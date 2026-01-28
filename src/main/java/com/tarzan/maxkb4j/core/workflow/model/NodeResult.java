package com.tarzan.maxkb4j.core.workflow.model;

import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.enums.WorkflowMode;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
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


    public void writeContext(AbsNode currentNode, Workflow workflow) {
        this.writeContextFunc.apply(nodeVariable, currentNode, workflow);
    }

    public void writeDetail(AbsNode currentNode) {
        this.writeDetailFunc.apply(nodeVariable, currentNode);
    }

    public boolean isInterruptExec(AbsNode currentNode) {
        return this.isInterrupt.apply(currentNode);
    }

    public boolean defaultIsInterrupt(AbsNode node) {
        return false;
    }

    public void defaultWriteContextFunc(Map<String, Object> nodeVariable, AbsNode node, Workflow workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
        }
        if(WorkflowMode.APPLICATION.equals(workflow.getWorkflowMode())){
            if (StringUtils.isNotBlank(node.getAnswerText())) {
                ChatMessageVO vo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        streamOutput?"":node.getAnswerText(),
                        "",
                        null,
                        false);
                workflow.getSink().tryEmitNext(vo);
                ChatMessageVO nodeEndVo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        "",
                        "",
                        null,
                        true);
                workflow.getSink().tryEmitNext(nodeEndVo);
            }
            workflow.setAnswer(workflow.getAnswer() + node.getAnswerText());
        }
    }

    public void defaultWriteDetailFunc(Map<String, Object> nodeVariable, AbsNode node) {
        if (nodeVariable != null) {
            if (NodeType.FORM.getKey().equals(node.getType())) {
                node.getDetail().put("form_data", nodeVariable.get("form_data"));
                node.getDetail().put("is_submit", nodeVariable.get("is_submit"));
            } else if (NodeType.VARIABLE_AGGREGATE.getKey().equals(node.getType())) {
                node.getDetail().put("result", nodeVariable);
            } else {
                node.getDetail().putAll(nodeVariable);
            }
        }
    }


    @FunctionalInterface
    public interface WriteContextFunction {
        void apply(Map<String, Object> nodeVariable, AbsNode node, Workflow workflow);
    }


    @FunctionalInterface
    public interface WriteDetailFunction {
        void apply(Map<String, Object> nodeVariable, AbsNode node);
    }

    @FunctionalInterface
    public interface IsInterruptFunction {
        boolean apply(AbsNode currentNode);
    }

    public boolean isAssertionResult() {
        return this.nodeVariable.containsKey("branchId");
    }


}