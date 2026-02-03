package com.tarzan.maxkb4j.module.tool.exception;

/**
 * 工具导入导出相关异常
 */
public class ToolImportExportException extends ToolException {

    public ToolImportExportException(String message) {
        super("TOOL_IMPORT_EXPORT_ERROR", message);
    }

    public ToolImportExportException(String message, Throwable cause) {
        super("TOOL_IMPORT_EXPORT_ERROR", message, cause);
    }
}
