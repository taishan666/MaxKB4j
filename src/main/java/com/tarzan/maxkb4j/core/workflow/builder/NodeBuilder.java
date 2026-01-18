package com.tarzan.maxkb4j.core.workflow.builder;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.*;

public class NodeBuilder {

    public static AbsNode getNode(LfNode lfNode) {
        if (lfNode == null) {
            throw new IllegalArgumentException("LfNode 不能为 null");
        }
        String id = lfNode.getId();
        String type = lfNode.getType();
        JSONObject properties = lfNode.getProperties();
        NodeType nodeType = NodeType.getByKey(type);
        if (nodeType == null) {
            throw new IllegalStateException("不支持的节点类型: " + type);
        }
        return switch (nodeType) {
            case START -> new StartNode(id,properties);
            case AI_CHAT -> new AiChatNode(id,properties);
            case SEARCH_KNOWLEDGE -> new SearchKnowledgeNode(id,properties);
            case CONDITION -> new ConditionNode(id,properties);
            case REPLY-> new DirectReplyNode(id,properties);
            case APPLICATION -> new ApplicationNode(id,properties);
            case QUESTION -> new QuestionNode(id,properties);
            case IMAGE_GENERATE -> new ImageGenerateNode(id,properties);
            case TEXT_TO_SPEECH -> new TextToSpeechNode(id,properties);
            case DOCUMENT_EXTRACT -> new DocumentExtractNode(id,properties);
            case DOCUMENT_SPLIT -> new DocumentSpiltNode(id,properties);
            case SPEECH_TO_TEXT -> new SpeechToTextNode(id,properties);
            case VARIABLE_ASSIGN -> new VariableAssignNode(id,properties);
            case VARIABLE_AGGREGATE -> new VariableAggregationNode(id,properties);
            case VARIABLE_SPLITTING -> new VariableSplittingNode(id,properties);
            case TOOL -> new ToolNode(id,properties);
            case TOOL_LIB -> new ToolLibNode(id,properties);
            case IMAGE_UNDERSTAND -> new ImageUnderstandNode(id,properties);
            case RERANKER -> new RerankerNode(id,properties);
            case FORM -> new FormNode(id,properties);
            case MCP -> new McpNode(id,properties);
            case NL2SQL -> new NL2SqlNode(id,properties);
            case INTENT_CLASSIFY -> new IntentClassifyNode(id,properties);
            case HTTP_CLIENT -> new HttpNode(id,properties);
            case PARAMETER_EXTRACTION -> new ParameterExtractionNode(id,properties);
            case USER_SELECT -> new UserSelectNode(id,properties);
            case DATA_SOURCE_LOCAL -> new DataSourceLocalNode(id,properties);
            case DATA_SOURCE_WEB -> new DataSourceWebNode(id,properties);
            case KNOWLEDGE_WRITE -> new KnowledgeWriteNode(id,properties);
            default -> null;
        };
    }


}
