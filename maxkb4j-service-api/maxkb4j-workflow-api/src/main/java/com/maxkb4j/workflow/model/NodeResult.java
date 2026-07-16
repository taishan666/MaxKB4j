package com.maxkb4j.workflow.model;

import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
        ChatMessageVO nodeEndVo = node.toChatMessageVO(
                workflow.getChatParams().getChatId(),
                workflow.getChatParams().getChatRecordId(),
                streamOutput?"":node.getAnswerText(),
                "",
                null,
                true);
        workflow.output().emit(nodeEndVo);
        // Sync update to workflow context
        workflow.context().appendNode(node);
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
        private IsInterruptFunction isInterrupt;
        private WriteContextFunction writeContextFunc;
        private WriteDetailFunction writeDetailFunc;
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


        // ==================== 快捷方法 ====================

        /**
         * 设置节点的回答文本（非流式）
         * <p>
         * 注意：方法名 {@code success} 仅表示"成功返回的答案"，与 {@link NodeResultFuture#getStatus()}
         * 的成功状态无关。{@code NodeResult} 本身不携带 status 概念。
         *
         * @param answer 答案内容
         * @return this builder
         */
        public Builder success(String answer) {
            this.variables.put("answer", answer);
            this.streamOutput = false;
            return this;
        }

        /**
         * 清空已添加的所有变量，并将流式输出重置为非流式
         * <p>
         * 注意：本方法会丢弃此前通过 {@link #variable(String, Object)} 添加的变量，
         * 通常用于需要重新开始的场景。如需创建全新的 Builder，建议直接 {@link NodeResult#builder()}。
         *
         * @return this builder
         */
        public Builder empty() {
            this.variables = new HashMap<>();
            this.streamOutput = false;
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