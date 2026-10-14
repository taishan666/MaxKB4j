package com.maxkb4j.common.domain.vo;

public abstract class ResultCallback<T>  {

    /**
     * Will be called as soon as the server replies.
     *
     * @param message The message body in a structured class.
     */
    public abstract void onEvent(T message);

    /** Will be called when all messages are received. */
    public abstract void onComplete();

    /**
     * Will be called when an Exception occurs.
     *
     * @param e The exception instance.
     */
    public abstract void onError(Throwable e);

    /**
     * 空回调：调用方不关心中间事件（如非流式对话、MCP tools/call）时使用，
     * 避免业务链路中对 callback 判空的遗漏导致 NPE。
     */
    @SuppressWarnings("unchecked")
    public static <T> ResultCallback<T> noop() {
        return (ResultCallback<T>) NoopCallback.INSTANCE;
    }

    private static final class NoopCallback<T> extends ResultCallback<T> {

        static final NoopCallback<Object> INSTANCE = new NoopCallback<>();

        @Override
        public void onEvent(T message) {
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void onError(Throwable e) {
        }
    }
}
