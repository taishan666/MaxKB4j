package com.maxkb4j.common.domain.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ChatInfoTest {

    @Test
    void addChatRecord_concurrentCallsWithSameId_keepSingleRecord() throws Exception {
        int threadCount = 32;
        ChatInfo chatInfo = new ChatInfo("chat-1", "app-1");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                int answer = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    ChatRecordDTO record = new ChatRecordDTO();
                    record.setId("record-1");
                    record.setAnswerText(String.valueOf(answer));
                    chatInfo.addChatRecord(record);
                    return null;
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertThat(chatInfo.getChatRecordList())
                .singleElement()
                .extracting(ChatRecordDTO::getId)
                .isEqualTo("record-1");
    }
}
