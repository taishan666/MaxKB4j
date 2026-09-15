package com.maxkb4j.common.exception;

/**
 * 接口限流超出异常，触发时返回 HTTP 429
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
