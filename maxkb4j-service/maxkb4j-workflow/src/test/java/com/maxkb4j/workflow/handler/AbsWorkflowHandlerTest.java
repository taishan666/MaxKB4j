package com.maxkb4j.workflow.handler;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.exception.NodeExceptionResolver;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.IWorkflowContext;
import com.maxkb4j.workflow.model.IWorkflowExecutionAccessor;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 工作流处理器模板回归测试：类型令牌分发（canHandle/execute 钩子收窄）、
 * 端到端两节点链执行（事件顺序）、默认失败钩子（异常解析链 + ERROR 状态）。
 */
class AbsWorkflowHandlerTest {

    private final NodeCenter nodeCenter = mock(NodeCenter.class);
    private final IWorkflow workflow = mock(IWorkflow.class);
    private final IWorkflowExecutionAccessor execution = mock(IWorkflowExecutionAccessor.class);
    private final IWorkflowContext context = mock(IWorkflowContext.class);
    private final INodeHandler nodeHandler = mock(INodeHandler.class);
    private final AtomicReference<Exception> resolvedException = new AtomicReference<>();

    /**
     * 记录事件的测试处理器：canHandle/execute 均由类型令牌 IWorkflow.class 绑定。
     */
    static class RecordingHandler extends AbsWorkflowHandler<IWorkflow> {
        final List<String> events = new ArrayList<>();

        RecordingHandler(NodeCenter nodeCenter, ExceptionResolverChain exceptionResolverChain) {
            super(IWorkflow.class, nodeCenter, exceptionResolverChain);
        }

        @Override
        protected void onProcessStart(IWorkflow workflow) {
            events.add("processStart");
        }

        @Override
        protected void onProcessCompleted(IWorkflow workflow) {
            events.add("processCompleted");
        }

        @Override
        public void onNodeStart(IWorkflow workflow, AbsNode node) {
            events.add("nodeStart:" + node.getId());
        }

        @Override
        public void onNodeSuccess(IWorkflow workflow, AbsNode node, NodeResult result) {
            events.add("nodeSuccess:" + node.getId());
        }
    }

    private RecordingHandler handler;

    @BeforeEach
    void setUp() {
        NodeExceptionResolver recorder = (wf, node, ex) -> {
            resolvedException.set(ex);
            return true;
        };
        handler = new RecordingHandler(nodeCenter, new ExceptionResolverChain(List.of(recorder)));
        when(workflow.execution()).thenReturn(execution);
        when(workflow.context()).thenReturn(context);
        when(workflow.getWorkflowMode()).thenReturn(WorkflowMode.APPLICATION);
        when(execution.currentNode()).thenReturn(null);
        when(execution.dependenciesNotExecuted(any())).thenReturn(false);
        when(nodeCenter.getHandler("test-node")).thenReturn(nodeHandler);
    }

    private static AbsNode newNode(String id) {
        AbsNode node = new AbsNode(id, new JSONObject()) {
        };
        node.setType("test-node");
        return node;
    }

    @Test
    void canHandleShouldFollowTypeToken() {
        assertThat(handler.canHandle(workflow)).isTrue();
        assertThat(handler.canHandle(mock(IWorkflow.class))).isTrue();
    }

    @Test
    void executeShouldRunChainAndFireTypedHooks() throws Exception {
        AbsNode first = newNode("n1");
        AbsNode second = newNode("n2");
        when(workflow.startNodes()).thenReturn(List.of(first));
        NodeResult firstResult = new NodeResult(Map.of("step", "one"));
        when(nodeHandler.execute(workflow, first)).thenReturn(CompletableFuture.completedFuture(firstResult));
        when(execution.nextNodes(first, firstResult)).thenReturn(List.of(second));
        when(nodeHandler.execute(workflow, second)).thenReturn(CompletableFuture.completedFuture(null));
        when(execution.nextNodes(second, null)).thenReturn(List.of());

        handler.execute(workflow);

        assertThat(handler.events).containsExactly(
                "processStart",
                "nodeStart:n1", "nodeSuccess:n1",
                "nodeStart:n2", "nodeSuccess:n2",
                "processCompleted");
        assertThat(first.getStatus()).isEqualTo(NodeStatus.SUCCESS.getStatus());
        assertThat(second.getStatus()).isEqualTo(NodeStatus.SUCCESS.getStatus());
        assertThat(first.getDetail()).containsEntry("step", "one");
    }

    @Test
    void executeShouldResumeFromCurrentNodeWhenPresent() throws Exception {
        AbsNode resumed = newNode("n1");
        when(execution.currentNode()).thenReturn(resumed);
        when(nodeHandler.execute(workflow, resumed))
                .thenReturn(CompletableFuture.completedFuture(new NodeResult(Map.of())));
        when(execution.nextNodes(any(), any())).thenReturn(List.of());

        handler.execute(workflow);

        assertThat(handler.events).containsExactly(
                "processStart",
                "nodeStart:n1", "nodeSuccess:n1",
                "processCompleted");
    }

    @Test
    void executeShouldResolveErrorAndMarkNodeErrorOnFailure() throws Exception {
        AbsNode node = newNode("n1");
        when(workflow.startNodes()).thenReturn(List.of(node));
        when(nodeHandler.execute(workflow, node))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("boom")));

        handler.execute(workflow);

        assertThat(node.getStatus()).isEqualTo(NodeStatus.ERROR.getStatus());
        assertThat(node.getDetail()).containsEntry(NodeField.EXCEPTION, "boom");
        assertThat(resolvedException.get()).isInstanceOf(IllegalStateException.class);
        // 节点已启动后失败：nodeStart 已触发，失败分支终止该链但流程仍正常收尾
        assertThat(handler.events).containsExactly("processStart", "nodeStart:n1", "processCompleted");
    }
}
