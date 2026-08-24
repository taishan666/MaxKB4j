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
        String IS_RESULT = "isResult";
        String REASONING_CONTENT = "reasoningContent";
        String REASONING_CONTENT_ENABLE = "reasoningContentEnable";
        String CONTENT = "content";
        String BRANCH_ID = "branchId";
        String BRANCH_NAME = "branchName";
        String RESULT_LIST = "resultList";
        String OTHER_LIST = "otherList";
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
        String FILE_LIST = "fileList";
        String VARIABLE_LIST = "variableList";
        String GROUP_LIST = "groupList";
        String HAS_IMAGES = "hasImages";
        String CONFIG = "config";
        String PARAMS = "params";
        String REQUEST = "request";
        String FIELD = "field";
        String FIELDS = "fields";
        String SOURCE = "source";
        String WRITE_CONTENT = "write_content";
        String CATEGORY = "category";
        String REASON = "reason";
        String STRATEGY = "strategy";
        String N = "n";
        String NEGATIVE_PROMPT = "negative_prompt";
        String MESSAGE_TOKENS = "messageTokens";
        String ANSWER_TOKENS = "answerTokens";
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
        String RUN_TIME = "runTime";
        String ERROR = "error";
        String ERROR_TIME = "errorTime";
        String ERROR_CLASS = "errorClass";
        String EXCEPTION_MESSAGE = "exceptionMessage";
    }

    /**
     * 模型配置字段键名
     */
    interface ModelField {
        String MODEL_ID = "modelId";
        String MODEL_PARAMS_SETTING = "modelParamsSetting";
    }

    /**
     * 知识库相关字段键名
     */
    interface KnowledgeField {
        String SPLIT_STRATEGY = "splitStrategy";
        String CHUNK_SIZE = "chunkSize";
        String SHOW_KNOWLEDGE = "showKnowledge";
    }

    /**
     * 检索结果元数据字段键名
     */
    interface MetadataField {
        String TITLE = "title";
        String SIMILARITY = "similarity";
        String KNOWLEDGE_TYPE = "knowledgeType";
        String KNOWLEDGE_NAME = "knowledgeName";
        String DOCUMENT_NAME = "documentName";
        String IS_ACTIVE = "isActive";
    }

    /**
     * 数据源节点字段键名
     */
    interface DataSourceField {
        String SELECTOR = "selector";
        String SOURCE_URL = "sourceUrl";
        String INPUT_PARAMS = "inputParams";
        String OUTPUT_PARAMS = "outputParams";
    }

    /**
     * 工具节点字段键名
     */
    interface ToolField {
        String MCP_TOOL = "mcpTool";
        String TOOL_PARAMS = "toolParams";
    }

    /**
     * 变量聚合策略键名
     */
    interface VariableStrategy {
        String FIRST_NON_NULL = "first_non_null";
        String VARIABLE_TO_JSON = "variable_to_json";
    }

    /**
     * 文档分段策略键名
     */
    interface DocumentStrategy {
        String QA = "qa";
    }

    /**
     * 用户选择节点字段键名
     */
    interface UserSelectField {
        String SELECT_CARD = "select-card";
    }

    /**
     * 条件组合逻辑键名
     */
    interface LogicField {
        String AND = "and";
        String OR = "or";
    }

    /**
     * 音频文件名拼接前缀/后缀
     */
    interface AudioField {
        String GENERATED_AUDIO_PREFIX = "generated_audio_";
        String MP3_SUFFIX = ".mp3";
    }

    /**
     * 表单/用户选择节点字段键名
     */
    interface FormField {
        String FORM = "form";
        String FORM_DATA = "form_data";
        String FORM_FIELD_LIST = "form_field_list";
        String FORM_CONTENT_FORMAT = "form_content_format";
        String IS_SUBMIT = "is_submit";
        String FORM_RENDER_TAG = "form_render";
        String CARD_SELECTION_RENDER_TAG = "card_selection_render";
    }

    /**
     * 答案展示类型（viewType）
     */
    interface ViewType {
        String SINGLE_VIEW = "single_view";
        String MANY_VIEW = "many_view";
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
        String LOOP_TYPE = "loop_type";
        String LOOP_TYPE_ARRAY = "ARRAY";
        String LOOP_TYPE_INFINITE = "LOOP";
        String NUMBER = "number";
        String BREAK = "BREAK";
        String CONTINUE = "continue";
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
        String CHAT_RECORD_ID = "chatRecordId";
        String HISTORY_MESSAGE = "historyMessage";
        String CHAT_FIELDS = "chatFields";
        String GLOBAL_FIELDS = "globalFields";
        String SYSTEM = "system";
        String TEXT = "text";
        String TYPE = "type";
        String IMAGE_URL = "image_url";
        String URL = "url";
        String TIME = "time";
        String HISTORY_CONTEXT = "historyContext";
        String CHAT_USER_ID = "chatUserId";
        String CHAT_USER_TYPE = "chatUserType";
        String CHAT_USER = "chatUser";
    }

    /**
     * HTTP 请求/响应字段键名
     */
    interface HttpField {
        String URL = "url";
        String STATUS = "status";
        String BODY = "body";
        String METHOD = "method";
        String HEADERS = "headers";
        String REQUEST_BODY = "requestBody";
        String PARAMS = "params";
        String TIMEOUT = "timeout";
    }

    /**
     * Spring Bean 名称
     */
    interface BeanName {
        String WORKFLOW_TASK_EXECUTOR = "workflowTaskExecutor";
    }

    /**
     * 业务消息码
     */
    interface MessageCode {
        String CONDITION_NO_MATCH = "workflow.condition.no.match";
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
