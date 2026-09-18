package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.IWorkflowExecutionAccessor;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.INode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 节点链调度器回归测试：链式递归、SKIP 节点交给 SKIP 传播、超时取消并经执行器上报、
 * 完成管线异常解包上报、单个节点调度失败不影响链（仅上报）。
 */
class NodeChainRunnerTest {

    @SuppressWarnings("unchecked")
    private final NodeLifecycleExecutor<IWorkflow> lifecycleExecutor = mock(NodeLifecycleExecutor.class);
    private final IWorkflow workflow = mock(IWorkflow.class);
    private final IWorkflowExecutionAccessor execution = mock(IWorkflowExecutionAccessor.class);

    private NodeChainRunner<IWorkflow> runner;

    @BeforeEach
    void setUp() {
        runner = new NodeChainRunner<>(lifecycleExecutor);
        when(workflow.execution()).thenReturn(execution);
        when(workflow.getNodeExecutionTimeoutMinutes()).thenReturn(60L);
    }

    private static AbsNode newNode(String id) {
        AbsNode node = new AbsNode(id, new JSONObject()) {
        };
        node.setType("test-node");
        return node;
    }

    @Test
    void runShouldExecuteSuccessorsRecursively() {
        AbsNode first = newNode("n1");
        AbsNode second = newNode("n2");
        when(lifecycleExecutor.execute(workflow, first))
                .thenReturn(CompletableFuture.completedFuture(List.of(second)));
        when(lifecycleExecutor.execute(workflow, second))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        runner.run(workflow, List.of(first));

        verify(lifecycleExecutor).execute(workflow, first);
        verify(lifecycleExecutor).execute(workflow, second);
        verifyNoMoreInteractions(lifecycleExecutor);
    }

    @Test
    void runShouldSkipEmptyOrNullBatches() {
        runner.run(workflow, List.of());
        runner.run(workflow, null);
        verifyNoMoreInteractions(lifecycleExecutor);
    }

    @Test
    void runShouldDelegateSkippedNodeToSkipPropagation() {
        AbsNode skipped = newNode("n1");
        skipped.setStatus(NodeStatus.SKIP.getStatus());
        when(lifecycleExecutor.skip(workflow, skipped))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        runner.run(workflow, List.of(skipped));

        verify(lifecycleExecutor).skip(workflow, skipped);
        verifyNoMoreInteractions(lifecycleExecutor);
    }

    @Test
    void runShouldCancelAndReportOnTimeout() {
        AbsNode node = newNode("slow-node");
        when(workflow.getNodeExecutionTimeoutMinutes()).thenReturn(0L);
        CompletableFuture<List<INode>> neverCompleting = new CompletableFuture<>();
        when(lifecycleExecutor.execute(workflow, node)).thenReturn(neverCompleting);

        runner.run(workflow, List.of(node));

        assertThat(neverCompleting).isCancelled();
        verify(lifecycleExecutor).reportError(same(workflow), same(node),
                argThat(ex -> ex instanceof TimeoutException && ex.getMessage().contains("timeout")));
    }

    @Test
    void runShouldUnwrapAndReportCompletionFailure() {
        AbsNode node = newNode("n1");
        when(lifecycleExecutor.execute(workflow, node))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("pipeline")));

        runner.run(workflow, List.of(node));

        verify(lifecycleExecutor).reportError(same(workflow), same(node),
                argThat(ex -> ex instanceof IllegalStateException && "pipeline".equals(ex.getMessage())));
    }

    @Test
    void runShouldReportSchedulingFailureWithoutBreakingChain() {
        AbsNode failing = newNode("bad");
        AbsNode healthy = newNode("good");
        when(lifecycleExecutor.execute(workflow, failing)).thenThrow(new IllegalStateException("schedule fail"));
        when(lifecycleExecutor.execute(workflow, healthy))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        runner.run(workflow, List.of(failing, healthy));

        verify(lifecycleExecutor).reportError(any(), same(failing),
                argThat(ex -> "schedule fail".equals(ex.getMessage())));
        verify(lifecycleExecutor).execute(workflow, healthy);
    }
}
