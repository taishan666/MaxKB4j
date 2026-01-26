package com.tarzan.maxkb4j.core.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeType {

    BASE("base-node", "基础节点"),

    START("start-node", "开始节点"),

    CONDITION("condition-node", "条件节点"),

    USER_SELECT("user-select-node", "用户选择节点"),

    ECHARTS("echarts-node", "基础图表节点"),

    DATABASE("database-node", "数据库节点"),

    CLASSIFICATION("classification-node", "分类节点"),

    QUESTION("question-node", "问题节点"),

    REPLY("reply-node", "回复节点"),

    SEARCH_KNOWLEDGE("search-knowledge-node", "知识库搜索节点"),

    FORM("form-node", "表单收集"),

    MCP("mcp-node", "MCP节点"),

    LOOP_NODE("loop-node","循环节点"),

    LOOP_START_NODE("loop-start-node","循环开始"),

    LOOP_CONTINUE_NODE("loop-continue-node","循环继续"),

    LOOP_BREAK_NODE("loop-break-node","循环跳出"),

    NL2SQL("nl2sql-node", "自然语言转SQL节点"),

    INTENT_CLASSIFY("intent-node", "意图分类"),

    RERANKER("reranker-node", "多路召回"),

    IMAGE_UNDERSTAND("image-understand-node", "图片理解"),

    IMAGE_GENERATE("image-generate-node", "图片生成"),

    TEXT_TO_SPEECH("text-to-speech-node", "文字转语音"),

    SPEECH_TO_TEXT("speech-to-text-node", "语音转文字"),

    DOCUMENT_EXTRACT("document-extract-node", "文档内容提取"),

    DOCUMENT_SPLIT("document-split-node", "文档分段节点"),

    VARIABLE_ASSIGN("variable-assign-node", "变量赋值"),

    VARIABLE_AGGREGATE("variable-aggregation-node", "变量聚合"),

    VARIABLE_SPLITTING("variable-splitting-node", "变量赋值"),

    TOOL("tool-node", "自定义函数节点"),

    TOOL_LIB("tool-lib-node", "工具库节点"),

    HTTP_CLIENT("http-node", "HTTP请求节点"),

    AI_CHAT("ai-chat-node", "智能聊天节点"),

    APPLICATION("application-node", "应用节点"),

    PARAMETER_EXTRACTION("parameter-extraction-node", "参数提取节点"),

    KNOWLEDGE_BASE("knowledge-base-node", "知识库基础节点"),

    DATA_SOURCE_LOCAL("data-source-local-node", "本地数据源"),

    DATA_SOURCE_WEB("data-source-web-node", "WEB数据源"),

    KNOWLEDGE_WRITE("knowledge-write-node", "知识库写入节点"),

    LOOP("loop-node", "循环节点"),
    ;

    private final String key;

    private final String name;


    public static NodeType getByKey(String key) {
        for (NodeType type : NodeType.values()) {
            if (type.getKey().equals(key)) {
                return type;
            }
        }
        return null;
    }

}
