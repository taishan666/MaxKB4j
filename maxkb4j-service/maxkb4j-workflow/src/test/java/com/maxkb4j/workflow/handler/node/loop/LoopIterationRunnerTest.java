package com.maxkb4j.workflow.handler.node.loop;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.impl.LoopNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: negative / zero / null loop counts must not crash the runner
 * ({@code new ArrayList<>(count)} throws IllegalArgumentException for negative
 * capacity); they simply run zero iterations.
 */
class LoopIterationRunnerTest {

    // count-loop type: any value other than ARRAY / LOOP falls back to counted loop
    private static final String LOOP_TYPE_COUNT = "COUNT";

    private LoopIterationRunner newRunner() {
        return new LoopIterationRunner(null, null, null, null);
    }

    private LoopNode newNode() {
        return new LoopNode("loop1", new JSONObject());
    }

    private LoopNode.NodeParams countParams(Integer number) {
        LoopNode.NodeParams params = new LoopNode.NodeParams();
        params.setLoopType(LOOP_TYPE_COUNT);
        params.setNumber(number);
        params.setLoopBody(new JSONObject());
        return params;
    }

    @Test
    void negativeCount_runsZeroIterationsWithoutCrash() {
        List<JSONObject> details = newRunner().run(null, newNode(), countParams(-1));
        assertThat(details).isEmpty();
    }

    @Test
    void integerMinCount_runsZeroIterationsWithoutCrash() {
        List<JSONObject> details = newRunner().run(null, newNode(), countParams(Integer.MIN_VALUE));
        assertThat(details).isEmpty();
    }

    @Test
    void zeroCount_runsZeroIterations() {
        List<JSONObject> details = newRunner().run(null, newNode(), countParams(0));
        assertThat(details).isEmpty();
    }

    @Test
    void nullCount_runsZeroIterations() {
        List<JSONObject> details = newRunner().run(null, newNode(), countParams(null));
        assertThat(details).isEmpty();
    }
}