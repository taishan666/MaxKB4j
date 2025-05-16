package com.tarzan.maxkb4j.core.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeType {

    BASE("base-node", "基础"),

    START("start-node", "开始节点"),

    CONDITION("condition-node", "条件节点"),

    USER_SELECT("user-select-node", "用户选择节点"),

    CLASSIFICATION("classification-node", "分类节点"),

    QUESTION("question-node", "问题节点"),

    REPLY("reply-node", "回复节点"),

    SEARCH_KNOWLEDGE("search-dataset-node", "知识库搜索节点"),

    FORM("form-node", "表单收集"),

    MCP("mcp-node", "MCP节点"),

    RERANKER("reranker-node", "多路召回"),

    IMAGE_UNDERSTAND("image-understand-node", "图片理解"),

    IMAGE_GENERATE("image-generate-node", "图片生成"),

    TEXT_TO_SPEECH("text-to-speech-node", "文字转语音"),

    SPEECH_TO_TEXT("speech-to-text-node", "语音转文字"),

    DOCUMENT_EXTRACT("document-extract-node", "文档内容提取"),

    VARIABLE_ASSIGN("variable-assign-node", "变量赋值"),

    FUNCTION("function-node", "自定义函数节点"),

    FUNCTION_LIB("function-lib-node", "函数库节点"),

    API("api-node", "api调用"),

    AI_CHAT("ai-chat-node", "智能聊天节点"),

    APPLICATION("application-node", "应用节点"),
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
