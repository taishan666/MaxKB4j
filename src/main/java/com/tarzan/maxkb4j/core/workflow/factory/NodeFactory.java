package com.tarzan.maxkb4j.core.workflow.factory;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.*;

import java.util.Objects;

public class NodeFactory {

    private static INode getNode(String id,String type, JSONObject properties) {
        NodeType nodeType = NodeType.getByKey(type);
        if (nodeType == null) {
            throw new IllegalStateException("不支持的节点类型: " + type);
        }
        return switch (nodeType) {
            case START -> new StartNode(properties);
            case AI_CHAT -> new AiChatNode(properties);
            case SEARCH_KNOWLEDGE -> new SearchKnowledgeNode(properties);
            case CONDITION -> new ConditionNode(properties);
            case REPLY-> new DirectReplyNode(properties);
            case APPLICATION -> new ApplicationNode(properties);
            case QUESTION -> new QuestionNode(properties);
            case IMAGE_GENERATE -> new ImageGenerateNode(properties);
            case TEXT_TO_SPEECH -> new TextToSpeechNode(properties);
            case DOCUMENT_EXTRACT -> new DocumentExtractNode(properties);
            case SPEECH_TO_TEXT -> new SpeechToTextNode(properties);
            case VARIABLE_ASSIGN -> new VariableAssignNode(properties);
            case VARIABLE_AGGREGATE -> new VariableAggregationNode(properties);
            case VARIABLE_SPLITTING -> new VariableSplittingNode(properties);
            case TOOL -> new ToolNode(properties);
            case TOOL_LIB -> new ToolLibNode(properties);
            case IMAGE_UNDERSTAND -> new ImageUnderstandNode(properties);
            case RERANKER -> new RerankerNode(properties);
            case FORM -> new FormNode(properties);
            case MCP -> new McpNode(properties);
            case NL2SQL -> new NL2SqlNode(properties);
            case INTENT_CLASSIFY -> new IntentClassifyNode(properties);
            case HTTP_CLIENT -> new HttpNode(properties);
            case PARAMETER_EXTRACTION -> new ParameterExtractionNode(properties);
            case USER_SELECT -> new UserSelectNode(properties);
            default -> null;
        };
    }

    public static INode getNode(LfNode lfNode) {
        INode node=getNode(lfNode.getId(),lfNode.getType(), lfNode.getProperties());
        if(Objects.nonNull(node)){
            node.setId(lfNode.getId());
            return node;
        }
        return null;
    }

}
