package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.node.aichat.impl.BaseChatNode;
import com.tarzan.maxkb4j.core.workflow.node.application.impl.BaseApplicationNode;
import com.tarzan.maxkb4j.core.workflow.node.condition.impl.BaseConditionNode;
import com.tarzan.maxkb4j.core.workflow.node.directreply.impl.BaseReplyNode;
import com.tarzan.maxkb4j.core.workflow.node.documentextract.impl.BaseDocumentExtractNode;
import com.tarzan.maxkb4j.core.workflow.node.formcollect.impl.FormNode;
import com.tarzan.maxkb4j.core.workflow.node.function.impl.BaseFunctionNode;
import com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl.BaseImageGenerateNode;
import com.tarzan.maxkb4j.core.workflow.node.imageunderstand.impl.BaseImageUnderstandNode;
import com.tarzan.maxkb4j.core.workflow.node.mcp.impl.BaseMcpNode;
import com.tarzan.maxkb4j.core.workflow.node.question.impl.BaseQuestionNode;
import com.tarzan.maxkb4j.core.workflow.node.reranker.impl.RerankerNode;
import com.tarzan.maxkb4j.core.workflow.node.searchdataset.impl.BaseSearchDatasetNode;
import com.tarzan.maxkb4j.core.workflow.node.speechtotext.impl.BaseSpeechToTextNode;
import com.tarzan.maxkb4j.core.workflow.node.start.impl.BaseStartNode;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.impl.BaseTextToSpeechNode;
import com.tarzan.maxkb4j.core.workflow.node.toollib.impl.ToolLibNode;
import com.tarzan.maxkb4j.core.workflow.node.variableaggregate.impl.BaseVariableAggregateNode;
import com.tarzan.maxkb4j.core.workflow.node.variableassign.impl.BaseVariableAssignNode;

import java.util.Objects;

public class NodeFactory {

    private static INode getNode(String type,JSONObject properties) {
        NodeType nodeType = NodeType.getByKey(type);
        if (nodeType == null) {
            throw new IllegalStateException("不支持的节点类型: " + type);
        }
        return switch (nodeType) {
            case START -> new BaseStartNode(properties);
            case AI_CHAT -> new BaseChatNode(properties);
            case SEARCH_KNOWLEDGE -> new BaseSearchDatasetNode(properties);
            case CONDITION -> new BaseConditionNode(properties);
            case REPLY-> new BaseReplyNode(properties);
            case APPLICATION -> new BaseApplicationNode(properties);
            case QUESTION -> new BaseQuestionNode(properties);
            case IMAGE_GENERATE -> new BaseImageGenerateNode(properties);
            case TEXT_TO_SPEECH -> new BaseTextToSpeechNode(properties);
            case DOCUMENT_EXTRACT -> new BaseDocumentExtractNode(properties);
            case SPEECH_TO_TEXT -> new BaseSpeechToTextNode(properties);
            case VARIABLE_ASSIGN -> new BaseVariableAssignNode(properties);
            case VARIABLE_AGGREGATE -> new BaseVariableAggregateNode(properties);
            case FUNCTION -> new BaseFunctionNode(properties);
            case TOOL_LIB -> new ToolLibNode(properties);
            case IMAGE_UNDERSTAND -> new BaseImageUnderstandNode(properties);
            case RERANKER -> new RerankerNode(properties);
            case FORM -> new FormNode(properties);
            case MCP -> new BaseMcpNode(properties);
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
