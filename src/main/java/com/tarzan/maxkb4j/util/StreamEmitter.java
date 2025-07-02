package com.tarzan.maxkb4j.util;


import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;


public class StreamEmitter {

    private final SseEmitter emitter;

    public StreamEmitter() {
        emitter = new SseEmitter(5 * 60 * 1000L);
    }

    public SseEmitter get() {
        return emitter;
    }

/*    public SseEmitter streaming(final ExecutorService executor, Runnable func) {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
        emitter.onCompletion(() -> {
            System.out.println("SseEmitter 完成");
            executor.shutdownNow();
        });
        emitter.onError((e) -> {
            System.out.println("SseEmitter 出现错误: " + e.getMessage());
            executor.shutdownNow();
        });

        emitter.onTimeout(() -> {
            System.out.println("SseEmitter 超时");
            emitter.complete();
            executor.shutdownNow();
        });
        executor.execute(() -> {
            try {
                func.run();
            } catch (Exception e) {
                System.out.println("捕获到异常: " + e.getMessage());
                emitter.completeWithError(e);
                Thread.currentThread().interrupt();
            } finally {
                if (!executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }
        });
        return emitter;
    }*/

 /*   public void over(Object obj) {
        this.send(obj);
        this.complete();
    }*/

    public void send(Object obj) {
        try {
            emitter.send(obj);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void complete() {
        emitter.complete();
    }

    public void error(Object message) {
        try {
            emitter.send(message);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
