package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.IWorkflowContext;
import com.maxkb4j.workflow.model.IWorkflowExecutionAccessor;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.INode;
import com.maxkb4j.workflow.registry.NodeCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 单节点执行管线回归测试：成功完成（状态/结果写入/事件顺序）、失败完成（异常上报、
 * 异常分支路由）、依赖未就绪短路、处理器同步抛异常收敛、SKIP 传播。
 */
class NodeLifecycleExecutorTest {

    private final NodeCenter nodeCenter = mock(NodeCenter.class);
    @SuppressWarnings("unchecked")
    private final NodeExecutionListener<IWorkflow> listener = mock(NodeExecutionListener.class);
    private final IWorkflow workflow = mock(IWorkflow.class);
    private final IWorkflowExecutionAccessor execution = mock(IWorkflowExecutionAccessor.class);
    private final IWorkflowContext context = mock(IWorkflowContext.class);
    private final INodeHandler nodeHandler = mock(INodeHandler.class);

    private NodeLifecycleExecutor<IWorkflow> executor;

    @BeforeEach
    void setUp() {
        executor = new NodeLifecycleExecutor<>(nodeCenter, listener);
        when(workflow.execution()).thenReturn(execution);
        when(workflow.context()).thenReturn(context);
        when(execution.dependenciesNotExecuted(any())).thenReturn(false);
        when(nodeCenter.getHandler("test-node")).thenReturn(nodeHandler);
    }

    private static AbsNode newNode(String id, JSONObject properties) {
        AbsNode node = new AbsNode(id, properties) {
        };
        node.setType("test-node");
        return node;
    }

    @Test
    void executeShouldCompleteSuccessLifecycle() throws Exception {
        AbsNode node = newNode("n1", new JSONObject());
        AbsNode next = newNode("n2", new JSONObject());
        NodeResult result = new NodeResult(Map.of("k", "v"));
        when(nodeHandler.execute(workflow, node)).thenReturn(CompletableFuture.completedFuture(result));
        when(execution.nextNodes(same(node), same(result))).thenReturn(List.of(next));

        List<INode> nextNodes = executor.execute(workflow, node).join();

        assertThat(nextNodes).containsExactly(next);
        assertThat(node.getStatus()).isEqualTo(NodeStatus.SUCCESS.getStatus());
        assertThat(node.getDetail())
                .containsEntry("k", "v")
                .containsKey(RuntimeDetailField.RUN_TIME);
        assertThat(node.getContext()).containsEntry("k", "v");
        verify(context).appendNode(node);
        InOrder lifecycle = inOrder(listener, execution, nodeHandler);
        lifecycle.verify(listener).onNodeStart(workflow, node);
        lifecycle.verify(execution).recordExecution(node);
        lifecycle.verify(nodeHandler).execute(workflow, node);
        lifecycle.verify(listener).onNodeSuccess(workflow, node, result);
    }

    @Test
    void executeShouldShortCircuitWhenDependenciesNotExecuted() throws Exception {
        AbsNode node = newNode("n1", new JSONObject());
        when(execution.dependenciesNotExecuted(node)).thenReturn(true);

        List<INode> nextNodes = executor.execute(workflow, node).join();

        assertThat(nextNodes).isEmpty();
        verifyNoInteractions(nodeHandler, listener);
    }

    @Test
    void executeShouldStopBranchOnFailureWithoutExceptionBranch() throws Exception {
        AbsNode node = newNode("n1", new JSONObject());
        when(nodeHandler.execute(workflow, node))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("boom")));

        List<INode> nextNodes = executor.execute(workflow, node).join();

        assertThat(nextNodes).isEmpty();
        verify(listener).onNodeError(same(workflow), same(node),
                argThat(ex -> ex instanceof IllegalStateException && "boom".equals(ex.getMessage())));
        assertThat(node.getDetail()).containsEntry(NodeField.EXCEPTION, "boom");
        verify(context).appendNode(node);
    }

    @Test
    void executeShouldRouteToExceptionBranchWhenEnabled() throws Exception {
        JSONObject properties = new JSONObject();
        properties.put(NodeField.ENABLE_EXCEPTION, true);
        AbsNode node = newNode("n1", properties);
        AbsNode branchNode = newNode("n2", new JSONObject());
        when(nodeHandler.execute(workflow, node))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("boom")));
        when(execution.nextNodes(same(node), any(NodeResult.class))).thenReturn(List.of(branchNode));

        List<INode> nextNodes = executor.execute(workflow, node).join();

        assertThat(nextNodes).containsExactly(branchNode);
        verify(listener).onNodeError(eq(workflow), same(node),
                argThat(ex -> "boom".equals(ex.getMessage())));
        verify(execution).nextNodes(same(node), argThat(result ->
                NodeField.EXCEPTION.equals(result.getNodeVariable().get(NodeField.BRANCH_ID))));
    }

    @Test
    void executeShouldFunnelSyncExceptionIntoFailurePath() throws Exception {
        AbsNode node = newNode("n1", new JSONObject());
        when(nodeHandler.execute(workflow, node)).thenThrow(new IllegalArgumentException("sync"));

        List<INode> nextNodes = executor.execute(workflow, node).join();

        assertThat(nextNodes).isEmpty();
        verify(listener).onNodeError(any(), any(),
                argThat(ex -> ex instanceof IllegalArgumentException && "sync".equals(ex.getMessage())));
    }

    @Test
    void skipShouldPropagateSkipStatusToSuccessors() {
        AbsNode node = newNode("n1", new JSONObject());
        AbsNode next = newNode("n2", new JSONObject());
        when(execution.nextNodes(same(node), any(NodeResult.class))).thenReturn(List.of(next));

        List<INode> nextNodes = executor.skip(workflow, node).join();

        assertThat(nextNodes).containsExactly(next);
        assertThat(next.getStatus()).isEqualTo(NodeStatus.SKIP.getStatus());
    }

    @Test
    void reportErrorShouldDelegateToListener() {
        AbsNode node = newNode("n1", new JSONObject());
        IllegalStateException ex = new IllegalStateException("wait fail");

        executor.reportError(workflow, node, ex);

        verify(listener).onNodeError(workflow, node, ex);
    }
}
