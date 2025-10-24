package com.tarzan.maxkb4j.core.workflow.factory;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.node.aichat.impl.AiChatNode;
import com.tarzan.maxkb4j.core.workflow.node.application.impl.ApplicationNode;
import com.tarzan.maxkb4j.core.workflow.node.condition.impl.ConditionNode;
import com.tarzan.maxkb4j.core.workflow.node.directreply.impl.DirectReplyNode;
import com.tarzan.maxkb4j.core.workflow.node.documentextract.impl.DocumentExtractNode;
import com.tarzan.maxkb4j.core.workflow.node.formcollect.impl.FormNode;
import com.tarzan.maxkb4j.core.workflow.node.http.impl.HttpNode;
import com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl.ImageGenerateNode;
import com.tarzan.maxkb4j.core.workflow.node.imageunderstand.impl.ImageUnderstandNode;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.impl.IntentClassifyNode;
import com.tarzan.maxkb4j.core.workflow.node.mcp.impl.McpNode;
import com.tarzan.maxkb4j.core.workflow.node.parameterextraction.impl.ParameterExtractionNode;
import com.tarzan.maxkb4j.core.workflow.node.question.impl.QuestionNode;
import com.tarzan.maxkb4j.core.workflow.node.reranker.impl.RerankerNode;
import com.tarzan.maxkb4j.core.workflow.node.searchknowledge.impl.SearchKnowledgeNode;
import com.tarzan.maxkb4j.core.workflow.node.speechtotext.impl.SpeechToTextNode;
import com.tarzan.maxkb4j.core.workflow.node.start.impl.StartNode;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.impl.TextToSpeechNode;
import com.tarzan.maxkb4j.core.workflow.node.tool.impl.ToolNode;
import com.tarzan.maxkb4j.core.workflow.node.toollib.impl.ToolLibNode;
import com.tarzan.maxkb4j.core.workflow.node.userselect.impl.UserSelectNode;
import com.tarzan.maxkb4j.core.workflow.node.variableassign.impl.VariableAssignNode;

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
