package com.tarzan.maxkb4j.common.exception;

/**
 * @BelongsProject: MaxKB4j
 * @BelongsPackage: com.tarzan.maxkb4j.common.exception
 * @Author: kaigejava
 * @CreateTime: 2026-03-05  21:50
 * @Description: 业务规则校验失败 400 Bad Request
 * @Version: 1.0
 */
public class FileLimitExceededException extends RuntimeException {
    public FileLimitExceededException(String message) {
        super(message);
    }
}
