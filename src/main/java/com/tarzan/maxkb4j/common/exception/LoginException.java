package com.tarzan.maxkb4j.common.exception;


/**
 * 登录异常
 *
 * @author tarzan liu
 * @date 2025年1月11日
 */

public class LoginException extends RuntimeException {



    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */

    public LoginException(String message) {
        super(message);
    }

}

