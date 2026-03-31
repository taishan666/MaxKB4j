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

    // ==================== Builder Pattern ====================

    /**
     * Create a new Builder for NodeResult.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for NodeResult.
     * Provides a fluent API for constructing NodeResult instances.
     */
    public static class Builder {
        private Map<String, Object> variables = new HashMap<>();
        private boolean streamOutput = false;
        private IsInterruptFunction isInterrupt = null;
        private WriteContextFunction writeContextFunc = null;
        private WriteDetailFunction writeDetailFunc = null;

        /**
         * Set the node variables.
         *
         * @param vars the variables map
         * @return this builder
         */
        public Builder variables(Map<String, Object> vars) {
            this.variables = vars != null ? vars : new HashMap<>();
            return this;
        }

        /**
         * Add a single variable.
         *
         * @param key   the variable key
         * @param value the variable value
         * @return this builder
         */
        public Builder variable(String key, Object value) {
            this.variables.put(key, value);
            return this;
        }

        /**
         * Set whether to stream output.
         *
         * @param stream true for stream output
         * @return this builder
         */
        public Builder streamOutput(boolean stream) {
            this.streamOutput = stream;
            return this;
        }

        /**
         * Set the interrupt function.
         *
         * @param func the interrupt function
         * @return this builder
         */
        public Builder interrupt(IsInterruptFunction func) {
            this.isInterrupt = func;
            return this;
        }

        /**
         * Set the write context function.
         *
         * @param func the write context function
         * @return this builder
         */
        public Builder writeContext(WriteContextFunction func) {
            this.writeContextFunc = func;
            return this;
        }

        /**
         * Set the write detail function.
         *
         * @param func the write detail function
         * @return this builder
         */
        public Builder writeDetail(WriteDetailFunction func) {
            this.writeDetailFunc = func;
            return this;
        }

        /**
         * Build the NodeResult instance.
         *
         * @return a new NodeResult instance
         */
        public NodeResult build() {
            NodeResult result = new NodeResult(variables, streamOutput, isInterrupt);
            if (writeContextFunc != null) {
                result.writeContextFunc = writeContextFunc;
            }
            if (writeDetailFunc != null) {
                result.writeDetailFunc = writeDetailFunc;
            }
            return result;
        }
    }


}