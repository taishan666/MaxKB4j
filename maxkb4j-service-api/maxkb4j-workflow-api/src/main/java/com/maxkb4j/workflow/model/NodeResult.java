package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

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
    public interface IsInterruptFunction {
        boolean apply(AbsNode currentNode);
    }

}
