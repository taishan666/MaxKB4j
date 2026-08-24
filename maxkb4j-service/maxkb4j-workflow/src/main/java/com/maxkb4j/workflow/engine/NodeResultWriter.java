package com.maxkb4j.workflow.engine;

import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;

/**
 * 节点结果写入器（实现层）。
 * <p>
 * 承载 {@link NodeResult} 契约中回调未指定时的引擎默认行为：
 * <ul>
 *   <li>写节点上下文（变量合并、chat 系节点结束消息、工作流上下文同步）</li>
 *   <li>写节点详情（变量聚合节点整体写入，其余平铺合并）</li>
 *   <li>中断判断与断言结果判断</li>
 * </ul>
 */
public final class NodeResultWriter {

    private NodeResultWriter() {
        // 工具类，不允许实例化
    }

    /**
     * 将节点结果写入节点上下文与工作流上下文。
     * <p>回调未指定时应用引擎默认实现。
     */
    public static void writeContext(NodeResult result, AbsNode node, IWorkflow workflow) {
        Map<String, Object> nodeVariable = result.getNodeVariable();
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
        }
        // Sync update to workflow context
        workflow.context().appendNode(node);
    }

    /**
     * 将节点结果写入节点运行时详情。
     * <p>回调未指定时应用引擎默认实现。
     */
    public static void writeDetail(NodeResult result, AbsNode node) {
        Map<String, Object> nodeVariable = result.getNodeVariable();
        if (nodeVariable != null) {
            if (NodeType.VARIABLE_AGGREGATE.getKey().equals(node.getType())) {
                node.getDetail().put(NodeField.RESULT, nodeVariable);
            } else {
                node.getDetail().putAll(nodeVariable);
            }
        }
    }

    /**
     * 判断节点结果是否要求中断执行。回调未指定时默认不中断。
     */
    public static boolean isInterruptExec(NodeResult result, AbsNode node) {
        NodeResult.IsInterruptFunction fn = result.getIsInterrupt();
        return fn != null && fn.apply(node);
    }

    /**
     * 判断节点结果是否为断言（分支）结果。
     */
    public static boolean isAssertionResult(NodeResult result) {
        Map<String, Object> nodeVariable = result.getNodeVariable();
        return nodeVariable != null && nodeVariable.containsKey(NodeField.BRANCH_ID);
    }
}
