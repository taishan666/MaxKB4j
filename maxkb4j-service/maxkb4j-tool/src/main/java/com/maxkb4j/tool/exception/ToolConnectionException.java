package com.maxkb4j.tool.exception;

/**
 * 工具连接测试相关异常
 */
public class ToolConnectionException extends ToolException {

    public ToolConnectionException(String message) {
        super("TOOL_CONNECTION_ERROR", message);
    }

    public ToolConnectionException(String message, Throwable cause) {
        super("TOOL_CONNECTION_ERROR", message, cause);
    }
}
