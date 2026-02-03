package com.tarzan.maxkb4j.module.tool.consts;

/**
 * 工具模块常量定义
 *
 * @author tarzan
 */
public interface ToolConstants {

    /**
     * 工具作用域
     */
    interface Scope {
        String WORKSPACE = "WORKSPACE";
    }

    /**
     * 工具类型
     */
    interface ToolType {
        String MCP = "MCP";
        String CUSTOM = "CUSTOM";
    }

    /**
     * 工具状态
     */
    interface Status {
        Boolean ACTIVE = true;
        Boolean INACTIVE = false;
    }

    /**
     * 文件相关常量
     */
    interface FileType {
        String TOOL_EXTENSION = ".tool";
        String JSON_CONTENT_TYPE = "application/json";
        String TEXT_CONTENT_TYPE = "text/plain";
    }

    /**
     * MCP服务器类型
     */
    interface McpType {
        String SSE = "sse";
        String STREAMABLE_HTTP = "streamable_http";
    }

    /**
     * 输入字段类型
     */
    interface InputType {
        String STRING = "string";
        String INTEGER = "int";
        String NUMBER = "number";
        String BOOLEAN = "boolean";
        String ARRAY = "array";
        String OBJECT = "object";
    }

    /**
     * 默认值
     */
    interface Defaults {
        String DEFAULT_FOLDER_ID = "default";
        String DEFAULT_VERSION = "1.0.0";
    }
}