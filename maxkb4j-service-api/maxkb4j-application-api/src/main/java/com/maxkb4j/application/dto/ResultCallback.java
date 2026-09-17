package com.maxkb4j.application.dto;

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
}
