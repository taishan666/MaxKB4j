package com.maxkb4j.tool.consts;

/**
 * 工具模块常量定义
 *
 * @author tarzan
 */
public final class ToolConstants {

    private ToolConstants() {
        // 工具类，不允许实例化
    }

    /**
     * 工具作用域
     */
    public static final class Scope {
        public static final String WORKSPACE = "WORKSPACE";

        private Scope() {
            // 工具类，不允许实例化
        }
    }

    /**
     * 工具类型
     */
    public static final class ToolType {
        public static final String MCP = "MCP";
        public static final String CUSTOM = "CUSTOM";
        public static final String HTTP = "HTTP";
        public static final String SKILL = "SKILL";
        public static final String DATA_SOURCE = "DATA_SOURCE";

        private ToolType() {
            // 工具类，不允许实例化
        }
    }

    /**
     * 工具状态
     */
    public static final class Status {
        public static final boolean ACTIVE = true;
        public static final boolean INACTIVE = false;

        private Status() {
            // 工具类，不允许实例化
        }
    }

    /**
     * 文件相关常量
     */
    public static final class FileType {
        public static final String TOOL_EXTENSION = ".tool";

        private FileType() {
            // 工具类，不允许实例化
        }
    }

    /**
     * MCP服务器类型
     */
    public static final class McpType {
        public static final String STDIO = "stdio";
        public static final String SSE = "sse";
        public static final String STREAMABLE_HTTP = "streamable_http";

        private McpType() {
            // 工具类，不允许实例化
        }
    }

    /**
     * 输入字段类型
     */
    public static final class InputType {
        public static final String STRING = "string";
        public static final String INTEGER = "int";
        public static final String NUMBER = "number";
        public static final String BOOLEAN = "boolean";
        public static final String ARRAY = "array";
        public static final String OBJECT = "object";

        private InputType() {
            // 工具类，不允许实例化
        }
    }

    /**
     * 默认值
     */
    public static final class Defaults {
        public static final String DEFAULT_FOLDER_ID = "default";
        public static final String DEFAULT_VERSION = "1.0.0";

        private Defaults() {
            // 工具类，不允许实例化
        }
    }
}
