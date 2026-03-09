package com.maxkb4j.tool.exception;

/**
 * 工具验证相关异常
 */
public class ToolValidationException extends ToolException {

    public ToolValidationException(String message) {
        super("TOOL_VALIDATION_ERROR", message);
    }

    public ToolValidationException(String message, Throwable cause) {
        super("TOOL_VALIDATION_ERROR", message, cause);
    }
}
