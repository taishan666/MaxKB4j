package com.maxkb4j.workflow.model;

import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
public class NodeResult {
    private Map<String, Object> nodeVariable;
    private boolean streamOutput;
    private WriteContextFunction writeContextFunc;
    private WriteDetailFunction writeDetailFunc;
    private IsInterruptFunction isInterrupt;

    /**
     * Builder for constructing NodeResult instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for NodeResult.
     */
    public static class Builder {
        private Map<String, Object> nodeVariable = new HashMap<>();
        private boolean streamOutput = false;
        private WriteContextFunction writeContextFunc;
        private WriteDetailFunction writeDetailFunc;
        private IsInterruptFunction isInterrupt;

        public Builder nodeVariable(Map<String, Object> nodeVariable) {
            this.nodeVariable = nodeVariable != null ? nodeVariable : new HashMap<>();
            return this;
        }

        public Builder putVariable(String key, Object value) {
            this.nodeVariable.put(key, value);
            return this;
        }

        public Builder streamOutput(boolean streamOutput) {
            this.streamOutput = streamOutput;
            return this;
        }

        public Builder writeContextFunc(WriteContextFunction writeContextFunc) {
            this.writeContextFunc = writeContextFunc;
            return this;
        }

        public Builder writeDetailFunc(WriteDetailFunction writeDetailFunc) {
            this.writeDetailFunc = writeDetailFunc;
            return this;
        }

        public Builder isInterrupt(IsInterruptFunction isInterrupt) {
            this.isInterrupt = isInterrupt;
            return this;
        }

        public NodeResult build() {
            NodeResult result = new NodeResult();
            result.nodeVariable = this.nodeVariable;
            result.streamOutput = this.streamOutput;
            result.writeContextFunc = this.writeContextFunc != null ? this.writeContextFunc : result::defaultWriteContextFunc;
            result.writeDetailFunc = this.writeDetailFunc != null ? this.writeDetailFunc : result::defaultWriteDetailFunc;
            result.isInterrupt = this.isInterrupt != null ? this.isInterrupt : result::defaultIsInterrupt;
            return result;
        }
    }

    // Legacy constructors for backward compatibility
    public NodeResult(Map<String, Object> nodeVariable) {
        this.nodeVariable = nodeVariable != null ? nodeVariable : new HashMap<>();
        this.streamOutput = false;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.writeDetailFunc = this::defaultWriteDetailFunc;
        this.isInterrupt = this::defaultIsInterrupt;
    }

    public NodeResult(Map<String, Object> nodeVariable, boolean streamOutput) {
        this.nodeVariable = nodeVariable != null ? nodeVariable : new HashMap<>();
        this.streamOutput = streamOutput;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.writeDetailFunc = this::defaultWriteDetailFunc;
        this.isInterrupt = this::defaultIsInterrupt;
    }


    public NodeResult(Map<String, Object> nodeVariable, boolean streamOutput, IsInterruptFunction isInterrupt) {
        this.nodeVariable = nodeVariable != null ? nodeVariable : new HashMap<>();
        this.streamOutput = streamOutput;
        this.writeContextFunc = this::defaultWriteContextFunc;
        this.writeDetailFunc = this::defaultWriteDetailFunc;
        this.isInterrupt = isInterrupt != null ? isInterrupt : this::defaultIsInterrupt;
    }

    // Default constructor for Builder
    private NodeResult() {
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
        // Use workflow's decision method instead of hardcoded checks
        if (workflow.needsSinkOutput()) {
            if (StringUtils.isNotBlank(node.getAnswerText())) {
                ChatMessageVO vo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        streamOutput ? "" : node.getAnswerText(),
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
        }
        // Sync update to workflow context
        workflow.getWorkflowContext().appendNode(node);
    }

    public void defaultWriteDetailFunc(Map<String, Object> nodeVariable, AbsNode node) {
        if (nodeVariable != null) {
            if (NodeType.VARIABLE_AGGREGATE.getKey().equals(node.getType())) {
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
        return this.nodeVariable != null && this.nodeVariable.containsKey("branchId");
    }


}