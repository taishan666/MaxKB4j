package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：批量分片处理流程。
 * 注意：BatchUtil.protectBach 同时存在 Consumer 与 Function 两个 2 参重载，
 * 这里用显式类型的函数式接口变量来消除重载歧义。
 */
class BatchUtilTest {

    @Test
    void protectBach_consumer_smallListExecutedAsSingleBatch() {
        List<Integer> list = IntStream.range(0, 5).boxed().collect(Collectors.toList());
        List<Integer> seenSizes = new ArrayList<>();
        Consumer<List<Integer>> recordSize = chunk -> seenSizes.add(chunk.size());
        BatchUtil.protectBach(list, recordSize);
        assertThat(seenSizes).containsExactly(5);
    }

    @Test
    void protectBach_consumer_customBatchSize() {
        List<Integer> list = IntStream.range(0, 5).boxed().collect(Collectors.toList());
        List<Integer> seenSizes = new ArrayList<>();
        Consumer<List<Integer>> recordSize = chunk -> seenSizes.add(chunk.size());
        BatchUtil.protectBach(list, 2, recordSize);
        assertThat(seenSizes).containsExactly(2, 2, 1);
    }

    @Test
    void protectBach_consumer_largeListSplitBy999() {
        List<Integer> list = IntStream.range(0, 1000).boxed().collect(Collectors.toList());
        List<Integer> seenSizes = new ArrayList<>();
        Consumer<List<Integer>> recordSize = chunk -> seenSizes.add(chunk.size());
        BatchUtil.protectBach(list, recordSize);
        assertThat(seenSizes).containsExactly(999, 1);
    }

    @Test
    void protectBach_consumer_nullAndEmptyAreNoOps() {
        int[] counter = {0};
        Consumer<List<Integer>> inc = chunk -> counter[0]++;
        BatchUtil.protectBach(null, inc);
        BatchUtil.protectBach(Collections.emptyList(), inc);
        assertThat(counter[0]).isZero();
    }

    @Test
    void protectBach_function_aggregatesResults() {
        List<Integer> list = IntStream.range(0, 5).boxed().collect(Collectors.toList());
        Function<List<Integer>, List<Integer>> mapper = chunk -> chunk.stream().map(i -> i * 10).collect(Collectors.toList());
        List<Integer> result = BatchUtil.protectBach(list, mapper);
        assertThat(result).containsExactly(0, 10, 20, 30, 40);
    }

    @Test
    void protectBach_function_largeListAggregatesInOrder() {
        List<Integer> list = IntStream.range(0, 1000).boxed().collect(Collectors.toList());
        Function<List<Integer>, List<Integer>> identity = chunk -> new ArrayList<>(chunk);
        List<Integer> result = BatchUtil.protectBach(list, identity);
        assertThat(result).hasSize(1000);
        assertThat(result).isEqualTo(list);
    }

    @Test
    void protectBach_function_nullAndEmptyReturnEmpty() {
        Function<List<Integer>, List<Integer>> identity = chunk -> new ArrayList<>(chunk);
        assertThat(BatchUtil.protectBach(null, identity)).isEmpty();
        assertThat(BatchUtil.protectBach(Collections.emptyList(), identity)).isEmpty();
    }

    @Test
    void protectBach_function_toleratesNullChunkResult() {
        List<Integer> list = IntStream.range(0, 3).boxed().collect(Collectors.toList());
        Function<List<Integer>, List<Integer>> returnsNull = chunk -> null;
        assertThat(BatchUtil.protectBach(list, returnsNull)).isEmpty();
    }
}