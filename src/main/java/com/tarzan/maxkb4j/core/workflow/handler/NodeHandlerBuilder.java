package com.tarzan.maxkb4j.core.workflow.handler;

import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.handler.node.impl.*;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class NodeHandlerBuilder {


    /**
     * INodeHandler缓存池
     */
    private static final Map<String, INodeHandler> HANDLER_POOL = new ConcurrentHashMap<>();

    static {
        HANDLER_POOL.put(NodeType.APPLICATION.getKey(), SpringUtil.getBean(ApplicationNodeHandler.class));
        HANDLER_POOL.put(NodeType.AI_CHAT.getKey(), SpringUtil.getBean(AiChatNodeHandler.class));
        HANDLER_POOL.put(NodeType.CONDITION.getKey(), SpringUtil.getBean(ConditionNodeHandler.class));
        HANDLER_POOL.put(NodeType.REPLY.getKey(), SpringUtil.getBean(DirectReplyNodeHandler.class));
        HANDLER_POOL.put(NodeType.DOCUMENT_EXTRACT.getKey(), SpringUtil.getBean(DocumentExtractNodeHandler.class));
        HANDLER_POOL.put(NodeType.FORM.getKey(), SpringUtil.getBean(FormNodeHandler.class));
        HANDLER_POOL.put(NodeType.HTTP_CLIENT.getKey(), SpringUtil.getBean(HttpNodeHandler.class));
        HANDLER_POOL.put(NodeType.IMAGE_GENERATE.getKey(), SpringUtil.getBean(ImageGenerateNodeHandler.class));
        HANDLER_POOL.put(NodeType.IMAGE_UNDERSTAND.getKey(), SpringUtil.getBean(ImageUnderstandNodeHandler.class));
        HANDLER_POOL.put(NodeType.INTENT_CLASSIFY.getKey(), SpringUtil.getBean(IntentClassifyNodeHandler.class));
        HANDLER_POOL.put(NodeType.MCP.getKey(), SpringUtil.getBean(McpNodeHandler.class));
        HANDLER_POOL.put(NodeType.QUESTION.getKey(), SpringUtil.getBean(QuestionNodeHandler.class));
        HANDLER_POOL.put(NodeType.RERANKER.getKey(), SpringUtil.getBean(RerankerNodeHandler.class));
        HANDLER_POOL.put(NodeType.SEARCH_KNOWLEDGE.getKey(), SpringUtil.getBean(SearchKnowledgeNodeHandler.class));
        HANDLER_POOL.put(NodeType.SPEECH_TO_TEXT.getKey(), SpringUtil.getBean(SpeechToTextNodeHandler.class));
        HANDLER_POOL.put(NodeType.START.getKey(), SpringUtil.getBean(StartNodeHandler.class));
        HANDLER_POOL.put(NodeType.TEXT_TO_SPEECH.getKey(), SpringUtil.getBean(TextToSpeechNodeHandler.class));
        HANDLER_POOL.put(NodeType.TOOL_LIB.getKey(), SpringUtil.getBean(ToolNodeHandler.class));
        HANDLER_POOL.put(NodeType.TOOL.getKey(), SpringUtil.getBean(ToolNodeHandler.class));
        HANDLER_POOL.put(NodeType.VARIABLE_ASSIGN.getKey(), SpringUtil.getBean(VariableAssignNodeHandler.class));
        HANDLER_POOL.put(NodeType.VARIABLE_AGGREGATE.getKey(), SpringUtil.getBean(VariableAggregationNodeHandler.class));
        HANDLER_POOL.put(NodeType.VARIABLE_SPLITTING.getKey(), SpringUtil.getBean(VariableSplittingNodeHandler.class));
        HANDLER_POOL.put(NodeType.PARAMETER_EXTRACTION.getKey(), SpringUtil.getBean(ParameterExtractionNodeHandler.class));
        HANDLER_POOL.put(NodeType.USER_SELECT.getKey(), SpringUtil.getBean(UserSelectNodeHandler.class));
    }

    /**
     * 获取NodeHandler
     *
     * @param nodeType 授权类型
     * @return INodeHandler
     */
    public static INodeHandler getHandler(String nodeType) {
        INodeHandler nodeHandler = HANDLER_POOL.get(nodeType);
        if (nodeHandler == null) {
            throw new ApiException("no grantType was found");
        } else {
            return HANDLER_POOL.getOrDefault(nodeType, HANDLER_POOL.get(NodeType.START.getKey()));
        }
    }
}
