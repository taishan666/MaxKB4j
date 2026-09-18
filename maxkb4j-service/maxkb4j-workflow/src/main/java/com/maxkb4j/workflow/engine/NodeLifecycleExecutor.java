package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.INode;
import com.maxkb4j.workflow.registry.NodeCenter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;

/**
 * 单节点执行管线（引擎层协作者）。
 * <p>
 * 承载一个节点的完整生命周期：依赖就绪检查、启动回调、节点处理器调用、执行计时、
 * 成功/失败完成语义（含异常分支路由）与 SKIP 传播，并持有 {@link NodeExecutionListener}
 * 作为节点事件的唯一出口（含调度/等待阶段失败的 {@link #reportError} 上报）。
 * 链路调度编排见 {@link NodeChainRunner}，结果写入委托 {@link NodeResultWriter}，
 * 节点处理器经 {@link NodeCenter} 解析，不依赖具体处理器层次。
 * </p>
 *
 * @param <W> 监听方接受的工作流契约
 */
@Slf4j
public class NodeLifecycleExecutor<W extends IWorkflow> {

    private final NodeCenter nodeCenter;
    private final NodeExecutionListener<W> listener;

    public NodeLifecycleExecutor(NodeCenter nodeCenter, NodeExecutionListener<W> listener) {
        this.nodeCenter = nodeCenter;
        this.listener = listener;
    }

    /**
     * 上报调度/等待阶段发现的节点失败，与执行阶段失败共用同一监听器出口，
     * 供 {@link NodeChainRunner} 在超时或等待异常时调用。
     */
    public void reportError(W workflow, AbsNode node, Exception ex) {
        listener.onNodeError(workflow, node, ex);
    }

    /**
     * 执行一个就绪节点：依赖未就绪时直接空完成；否则启动回调 → 记录执行 → 调用节点处理器，
     * 在节点处理器返回的 future 上链式完成，成功与失败收敛到同一完成路径。
     */
    public CompletableFuture<List<INode>> execute(W workflow, AbsNode node) {
        if (workflow.execution().dependenciesNotExecuted(node)) {
            return CompletableFuture.completedFuture(List.of());
        }
        listener.onNodeStart(workflow, node);
        workflow.execution().recordExecution(node);
        INodeHandler nodeHandler = nodeCenter.getHandler(node.getType());
        long startTime = System.currentTimeMillis();
        return startNodeExecution(nodeHandler, workflow, node)
                .handle((result, ex) -> complete(workflow, node, startTime, result, ex));
    }

    /**
     * 传播已跳过节点的 SKIP 状态到其下游节点。
     */
    public CompletableFuture<List<INode>> skip(W workflow, AbsNode node) {
        List<INode> nextNodeList = workflow.execution().nextNodes(node, new NodeResult(Map.of()));
        nextNodeList.forEach(nextNode -> nextNode.setStatus(NodeStatus.SKIP.getStatus()));
        return CompletableFuture.completedFuture(nextNodeList);
    }

    /**
     * 解包 {@link CompletionException}/{@link ExecutionException} 链中的真实异常原因，
     * 非 {@link Exception} 的 throwable 转为 {@link RuntimeException}。
     */
    static Exception unwrapException(Throwable cause) {
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
    }

    /**
     * 调用节点处理器，将同步抛出的异常汇入异常完成的 future，
     * 使两种失败模式收敛到单一完成路径。
     */
    private CompletableFuture<NodeResult> startNodeExecution(INodeHandler nodeHandler, W workflow, AbsNode node) {
        try {
            return nodeHandler.execute(workflow, node);
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    /**
     * 完成异步执行的节点：记录执行耗时，失败转为失败完成语义，成功交由成功完成语义处理。
     */
    private List<INode> complete(W workflow, AbsNode node, long startTime, NodeResult result, Throwable ex) {
        recordExecutionTime(node, startTime);
        if (ex != null) {
            return completeFailedNode(workflow, node, ex);
        }
        return completeNode(workflow, node, result);
    }

    /**
     * 成功完成：应用 SUCCESS 状态、写入结果、触发成功回调并解析下一节点。
     */
    private List<INode> completeNode(W workflow, AbsNode node, NodeResult result) {
        node.setStatus(NodeStatus.SUCCESS.getStatus());
        if (result != null) {
            writeResult(result, node, workflow);
        }
        listener.onNodeSuccess(workflow, node, result);
        return workflow.execution().nextNodes(node, result);
    }

    /**
     * 失败完成：节点启用异常分支时路由到 exception 分支，
     * 否则经监听器上报异常并以空后继列表终止该分支。
     */
    private List<INode> completeFailedNode(W workflow, AbsNode node, Throwable ex) {
        listener.onNodeError(workflow, node, unwrapException(ex));
        NodeResult result = new NodeResult(Map.of(NodeField.EXCEPTION, ex.getMessage()));
        Boolean enableException = node.getProperties().getBoolean(NodeField.ENABLE_EXCEPTION);
        if (Boolean.TRUE.equals(enableException)) {
            result = new NodeResult(Map.of(
                    NodeField.BRANCH_ID, NodeField.EXCEPTION,
                    NodeField.EXCEPTION, ex.getMessage()));
            return workflow.execution().nextNodes(node, result);
        }
        writeResult(result, node, workflow);
        return List.of();
    }

    /**
     * 将节点结果写入节点详情与工作流上下文。
     */
    private void writeResult(NodeResult result, AbsNode node, IWorkflow workflow) {
        NodeResultWriter.writeDetail(result, node);
        NodeResultWriter.writeContext(result, node, workflow);
    }

    /**
     * 记录节点执行耗时（秒）到节点详情。
     */
    private void recordExecutionTime(AbsNode node, long startTime) {
        float runTime = (System.currentTimeMillis() - startTime) / 1000F;
        node.getDetail().put(RuntimeDetailField.RUN_TIME, runTime);
        log.info("node: {}, runTime: {} s", resolveNodeName(node), runTime);
    }

    private static String resolveNodeName(AbsNode node) {
        JSONObject properties = node.getProperties();
        if (properties == null) {
            return node.getType();
        }
        String nodeName = properties.getString(RuntimeDetailField.NODE_NAME);
        return nodeName != null ? nodeName : node.getType();
    }
}
