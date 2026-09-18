package com.maxkb4j.workflow.engine;

import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.INode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 节点链调度器（引擎层协作者）。
 * <p>
 * 批量调度一批节点并在调用线程上逐个等待其完成 future（含超时取消），随后递归执行
 * 后继节点链。单节点的执行、完成语义与节点事件出口统一由 {@link NodeLifecycleExecutor}
 * 承担：调度与等待期的失败经 {@link NodeLifecycleExecutor#reportError} 上报，
 * 不中断其余节点的调度。
 * </p>
 *
 * @param <W> 执行的工作流契约
 */
@Slf4j
public class NodeChainRunner<W extends IWorkflow> {

    private final NodeLifecycleExecutor<W> lifecycleExecutor;

    public NodeChainRunner(NodeLifecycleExecutor<W> lifecycleExecutor) {
        this.lifecycleExecutor = lifecycleExecutor;
    }

    /**
     * 逐个等待批次内节点的完成并递归执行后继链。
     */
    public void run(W workflow, List<INode> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) {
            return;
        }
        long timeoutMinutes = workflow.getNodeExecutionTimeoutMinutes();
        for (ScheduledNode scheduled : schedule(workflow, nodeList)) {
            AbsNode node = scheduled.node();
            try {
                List<INode> nextNodeList = scheduled.future().get(timeoutMinutes, TimeUnit.MINUTES);
                run(workflow, nextNodeList);
            } catch (TimeoutException e) {
                scheduled.future().cancel(true);
                lifecycleExecutor.reportError(workflow, node, new TimeoutException(
                        "node [" + node.getType() + "] execution timeout after " + timeoutMinutes + " minutes"));
            } catch (ExecutionException e) {
                lifecycleExecutor.reportError(workflow, node, NodeLifecycleExecutor.unwrapException(e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for node [{}] execution", node.getType());
                break;
            }
        }
    }

    /**
     * 调度一批节点，将每个节点与其后继列表 future 配对：
     * READY 节点交给执行管线异步执行，SKIP 节点传播跳过状态；
     * 单个节点调度失败经执行器上报，不影响其余节点。
     */
    private List<ScheduledNode> schedule(W workflow, List<INode> nodeList) {
        List<ScheduledNode> scheduledNodes = new ArrayList<>(nodeList.size());
        for (INode entry : nodeList) {
            // 契约节点在引擎内统一为 AbsNode（INode 的引擎实现）
            AbsNode node = (AbsNode) entry;
            try {
                if (NodeStatus.READY.getStatus() == node.getStatus()) {
                    scheduledNodes.add(new ScheduledNode(node, lifecycleExecutor.execute(workflow, node)));
                } else if (NodeStatus.SKIP.getStatus() == node.getStatus()) {
                    scheduledNodes.add(new ScheduledNode(node, lifecycleExecutor.skip(workflow, node)));
                }
            } catch (Exception e) {
                lifecycleExecutor.reportError(workflow, node, e);
            }
        }
        return scheduledNodes;
    }

    /**
     * 批次内被调度的节点，与其后继列表的 future 配对。
     */
    private record ScheduledNode(AbsNode node, CompletableFuture<List<INode>> future) {
    }
}
