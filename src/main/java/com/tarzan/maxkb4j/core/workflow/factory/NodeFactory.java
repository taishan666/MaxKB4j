package com.tarzan.maxkb4j.core.workflow.factory;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.AiChatNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ApplicationNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ConditionNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DirectReplyNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DocumentExtractNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.FormNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.HttpNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ImageGenerateNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ImageUnderstandNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.IntentClassifyNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.McpNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ParameterExtractionNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.QuestionNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.RerankerNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.SearchKnowledgeNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.SpeechToTextNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.StartNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.TextToSpeechNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ToolNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ToolLibNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.UserSelectNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.VariableAssignNode;

import java.util.Objects;

public class NodeFactory {

    private static INode getNode(String type, JSONObject properties) {
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
            case TOOL -> new ToolNode(properties);
            case TOOL_LIB -> new ToolLibNode(properties);
            case IMAGE_UNDERSTAND -> new ImageUnderstandNode(properties);
            case RERANKER -> new RerankerNode(properties);
            case FORM -> new FormNode(properties);
            case MCP -> new McpNode(properties);
            case INTENT_CLASSIFY -> new IntentClassifyNode(properties);
            case HTTP_CLIENT -> new HttpNode(properties);
            case PARAMETER_EXTRACTION -> new ParameterExtractionNode(properties);
            case USER_SELECT -> new UserSelectNode(properties);
            default -> null;
        };
    }

    public static INode getNode(LfNode lfNode) {
        INode node=getNode(lfNode.getType(), lfNode.getProperties());
        if(Objects.nonNull(node)){
            node.setId(lfNode.getId());
            return node;
        }
        return null;
    }

}
