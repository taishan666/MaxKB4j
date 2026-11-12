package com.maxkb4j.common.util;

/**
 * 线程安全的流式文本累积器，避免按 Token 追加时复制已有内容。
 */
public final class StreamTextAccumulator {

    private final StringBuilder builder = new StringBuilder();

    public synchronized void append(String text) {
        builder.append(text);
    }

    @Override
    public synchronized String toString() {
        return builder.toString();
    }
}
