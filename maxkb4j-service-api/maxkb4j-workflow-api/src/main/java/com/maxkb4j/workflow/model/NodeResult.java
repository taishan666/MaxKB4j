package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;

/**
 * 节点执行结果（契约层）。
 * <p>
 * 承载节点输出的变量、流式标记与可自定义的行为回调规格；
 * 引擎侧的默认写入行为（写上下文/写详情/中断判断/断言判断）由实现模块的
 * {@code NodeResultWriter} 承载，回调未指定时由其应用引擎默认实现。
 */
@Slf4j
@Data
public class NodeResult {
    private Map<String, Object> nodeVariable;
    private boolean streamOutput;
    private WriteContextFunction writeContextFunc;
    private WriteDetailFunction writeDetailFunc;
    private IsInterruptFunction isInterrupt;

    public NodeResult(Map<String, Object> nodeVariable) {
        this.nodeVariable = nodeVariable != null ? nodeVariable : new HashMap<>();
        this.streamOutput = false;
    }

    public NodeResult(Map<String, Object> nodeVariable, boolean streamOutput) {
        this.nodeVariable = nodeVariable != null ? nodeVariable : new HashMap<>();
        this.streamOutput = streamOutput;
    }


    public NodeResult(Map<String, Object> nodeVariable, boolean streamOutput, IsInterruptFunction isInterrupt) {
        this.nodeVariable = nodeVariable != null ? nodeVariable : new HashMap<>();
        this.streamOutput = streamOutput;
        this.isInterrupt = isInterrupt;
    }

    @FunctionalInterface
    public interface WriteContextFunction {
        void apply(Map<String, Object> nodeVariable, AbsNode node, IWorkflow workflow);
    }


    @FunctionalInterface
    public interface WriteDetailFunction {
        void apply(Map<String, Object> nodeVariable, AbsNode node);
    }

    @FunctionalInterface
    public interface IsInterruptFunction {
        boolean apply(AbsNode currentNode);
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
         * 注意：方法名 {@code success} 仅表示"成功返回的答案"，与节点的执行状态
         * （{@code NodeStatus}）无关。{@code NodeResult} 本身不携带 status 概念。
         *
         * @param answer 答案内容
         * @return this builder
         */
        public Builder success(String answer) {
            this.variables.put(NodeField.ANSWER, answer);
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
