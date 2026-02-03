package com.tarzan.maxkb4j.module.tool.exception;

import lombok.Getter;

/**
 * 工具模块自定义异常
 *
 * @author tarzan
 */
@Getter
public class ToolException extends RuntimeException {

    private final String errorCode;

    public ToolException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ToolException(String errorCode,String message, Throwable cause) {
        super(message, cause);
        this.errorCode =errorCode;
    }

}



