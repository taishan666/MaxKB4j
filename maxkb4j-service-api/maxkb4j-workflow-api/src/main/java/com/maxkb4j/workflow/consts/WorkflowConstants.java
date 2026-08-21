package com.maxkb4j.workflow.consts;

/**
 * 工作流模块常量定义
 * <p>
 * 集中管理工作流中节点上下文、运行时详情、表单、循环、变量等字段的键名，
 * 避免魔法字符串散落在各节点与处理器中。
 *
 * @author tarzan
 */
public interface WorkflowConstants {

    /**
     * 节点上下文与输出字段键名
     */
    interface NodeField {
        String QUESTION = "question";
        String ANSWER = "answer";
        String RESULT = "result";
        String REASONING_CONTENT = "reasoningContent";
        String CONTENT = "content";
        String BRANCH_ID = "branchId";
        String BRANCH_NAME = "branchName";
        String RESULT_LIST = "resultList";
        String PARAGRAPH_LIST = "paragraphList";
        String DOCUMENT_LIST = "documentList";
        String IMAGE_LIST = "imageList";
        String AUDIO_LIST = "audioList";
        String IMAGE = "image";
        String DOCUMENT = "document";
        String AUDIO = "audio";
        String OTHER = "other";
        String DATA = "data";
        String SQL = "sql";
        String RELEVANCE_SCORE = "relevanceScore";
        String DIRECTLY_RETURN = "directlyReturn";
        String IS_HIT_HANDLING_METHOD_LIST = "isHitHandlingMethodList";
        String IS_INTERRUPT_EXEC = "is_interrupt_exec";
    }

    /**
     * 节点运行时详情字段键名
     */
    interface RuntimeDetailField {
        String INDEX = "index";
        String NODE_ID = "nodeId";
        String NODE_NAME = "nodeName";
        String NAME = "name";
        String NODE_DATA = "nodeData";
        String UP_NODE_ID_LIST = "upNodeIdList";
        String RUNTIME_NODE_ID = "runtimeNodeId";
        String TYPE = "type";
        String STATUS = "status";
        String ERR_MESSAGE = "errMessage";
    }

    /**
     * 表单/用户选择节点字段键名
     */
    interface FormField {
        String FORM_DATA = "form_data";
        String FORM_FIELD_LIST = "form_field_list";
        String FORM_CONTENT_FORMAT = "form_content_format";
        String IS_SUBMIT = "is_submit";
    }

    /**
     * 循环节点字段键名
     */
    interface LoopField {
        String LOOP_NODE_DATA = "loop_node_data";
        String CURRENT_INDEX = "current_index";
        String CURRENT_ITEM = "current_item";
        String ITEM = "item";
        String IS_BREAK = "is_break";
        String IS_CONTINUE = "is_continue";
        String LOOP_INPUT_FIELD_LIST = "loopInputFieldList";
    }

    /**
     * 变量字段键名
     */
    interface VariableField {
        String REFERENCE = "reference";
        String KEY = "key";
        String VALUE = "value";
        String NAME = "name";
        String INPUT_VALUE = "input_value";
        String OUTPUT_VALUE = "output_value";
    }

    /**
     * 聊天消息相关字段键名
     */
    interface ChatField {
        String CHAT_ID = "chatId";
        String HISTORY_MESSAGE = "historyMessage";
        String CHAT_FIELDS = "chatFields";
        String GLOBAL_FIELDS = "globalFields";
        String SYSTEM = "system";
        String TEXT = "text";
        String TYPE = "type";
        String IMAGE_URL = "image_url";
        String URL = "url";
    }

    /**
     * HTTP 请求/响应字段键名
     */
    interface HttpField {
        String URL = "url";
        String STATUS = "status";
        String BODY = "body";
    }

    /**
     * 变量作用域
     */
    interface Scope {
        String GLOBAL = "global";
        String CHAT = "chat";
        String LOOP = "loop";
        String GLOBAL_PREFIX = "global.";
        String CHAT_PREFIX = "chat.";
        String LOOP_PREFIX = "loop.";
    }

    /**
     * 默认占位值
     */
    interface Defaults {
        String NONE = "None";
    }
}
