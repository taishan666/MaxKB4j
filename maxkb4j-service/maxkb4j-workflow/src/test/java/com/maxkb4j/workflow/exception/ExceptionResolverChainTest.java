package com.maxkb4j.workflow.exception;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.exception.impl.DetailRecordingResolver;
import com.maxkb4j.workflow.exception.impl.LoggingExceptionResolver;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试:异常解析器责任链(按 order 排序、continue/break、解析器异常吞掉、详情记录)。
 */
class ExceptionResolverChainTest {

    private AbsNode newNode() {
        return new AbsNode("n1", new JSONObject()) {};
    }

    /** 记录执行顺序并可控制返回值 / 是否抛异常的测试解析器 */
    private static class RecordingResolver implements NodeExceptionResolver {
        private final int order;
        private final boolean shouldContinue;
        private final RuntimeException failure;
        private final List<String> sink;

        RecordingResolver(int order, boolean shouldContinue, RuntimeException failure, List<String> sink) {
            this.order = order;
            this.shouldContinue = shouldContinue;
            this.failure = failure;
            this.sink = sink;
        }

        @Override
        public boolean resolve(IWorkflow workflow, AbsNode node, Exception ex) {
            sink.add("R" + order);
            if (failure != null) {
                throw failure;
            }
            return shouldContinue;
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    @Test
    void constructor_sortsResolversByOrderAscending() {
        List<String> order = new ArrayList<>();
        NodeExceptionResolver high = new RecordingResolver(50, true, null, order);
        NodeExceptionResolver low = new RecordingResolver(10, true, null, order);
        NodeExceptionResolver mid = new RecordingResolver(30, true, null, order);

        new ExceptionResolverChain(List.of(high, low, mid))
                .resolve(null, newNode(), new RuntimeException("boom"));

        assertThat(order).containsExactly("R10", "R30", "R50");
    }

    @Test
    void resolve_runsAllResolversWhenAllReturnTrue() {
        List<String> order = new ArrayList<>();
        ExceptionResolverChain chain = new ExceptionResolverChain(List.of(
                new RecordingResolver(1, true, null, order),
                new RecordingResolver(2, true, null, order),
                new RecordingResolver(3, true, null, order)));

        chain.resolve(null, newNode(), new RuntimeException("boom"));

        assertThat(chain.size()).isEqualTo(3);
        assertThat(order).containsExactly("R1", "R2", "R3");
    }

    @Test
    void resolve_stopsChainWhenResolverReturnsFalse() {
        List<String> order = new ArrayList<>();
        ExceptionResolverChain chain = new ExceptionResolverChain(List.of(
                new RecordingResolver(1, true, null, order),
                new RecordingResolver(2, false, null, order),
                new RecordingResolver(3, true, null, order)));

        chain.resolve(null, newNode(), new RuntimeException("boom"));

        assertThat(order).containsExactly("R1", "R2");
    }

    @Test
    void resolve_swallowsResolverExceptionAndContinues() {
        List<String> order = new ArrayList<>();
        ExceptionResolverChain chain = new ExceptionResolverChain(List.of(
                new RecordingResolver(1, true, new IllegalStateException("resolver blew up"), order),
                new RecordingResolver(2, true, null, order)));

        chain.resolve(null, newNode(), new RuntimeException("boom"));

        // 解析器 1 抛异常被吞掉,链继续到解析器 2
        assertThat(order).containsExactly("R1", "R2");
    }

    @Test
    void detailRecordingResolver_recordsErrorOnNode() {
        DetailRecordingResolver resolver = new DetailRecordingResolver();
        AbsNode node = newNode();
        IllegalStateException ex = new IllegalStateException("node failed");

        boolean shouldContinue = resolver.resolve(null, node, ex);

        assertThat(shouldContinue).isTrue();
        assertThat(node.getErrMessage()).isEqualTo("node failed");
        assertThat(node.getDetail().get("error")).isEqualTo("node failed");
        assertThat(node.getDetail().get("errorClass")).isEqualTo("IllegalStateException");
        assertThat(node.getDetail()).containsKey("errorTime");
    }

    @Test
    void chainWithRealResolvers_executesBothAndRecordsError() {
        ExceptionResolverChain chain = new ExceptionResolverChain(List.of(
                new DetailRecordingResolver(),
                new LoggingExceptionResolver()));
        AbsNode node = newNode();

        chain.resolve(null, node, new RuntimeException("boom"));

        // DetailRecordingResolver(order 2) 记录错误;LoggingExceptionResolver(order 1) 先执行且返回 true 未中断链
        assertThat(chain.size()).isEqualTo(2);
        assertThat(node.getErrMessage()).isEqualTo("boom");
        assertThat(node.getDetail().get("error")).isEqualTo("boom");
        assertThat(node.getDetail().get("errorClass")).isEqualTo("RuntimeException");
    }

    @Test
    void emptyChain_resolvesWithoutEffect() {
        ExceptionResolverChain chain = new ExceptionResolverChain(List.of());
        AbsNode node = newNode();

        chain.resolve(null, node, new RuntimeException("boom"));

        assertThat(chain.size()).isZero();
        assertThat(node.getErrMessage()).isEqualTo("");
    }
}