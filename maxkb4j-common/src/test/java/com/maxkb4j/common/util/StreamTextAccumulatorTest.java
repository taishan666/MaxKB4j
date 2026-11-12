package com.maxkb4j.common.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class StreamTextAccumulatorTest {

    @Test
    void appendsTextInOrder() {
        StreamTextAccumulator accumulator = new StreamTextAccumulator();

        accumulator.append("hello");
        accumulator.append(" ");
        accumulator.append("world");

        assertThat(accumulator.toString()).isEqualTo("hello world");
    }

    @Test
    void supportsConcurrentAppends() throws Exception {
        int threadCount = 8;
        int appendsPerThread = 1_000;
        StreamTextAccumulator accumulator = new StreamTextAccumulator();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                char token = (char) ('A' + i);
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int j = 0; j < appendsPerThread; j++) {
                        accumulator.append(String.valueOf(token));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        String result = accumulator.toString();
        assertThat(result).hasSize(threadCount * appendsPerThread);
        for (int i = 0; i < threadCount; i++) {
            char token = (char) ('A' + i);
            assertThat(result.chars().filter(c -> c == token).count()).isEqualTo(appendsPerThread);
        }
    }
}
