package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.node.aichat.impl.BaseChatNode;
import com.tarzan.maxkb4j.core.workflow.node.application.impl.BaseApplicationNode;
import com.tarzan.maxkb4j.core.workflow.node.classification.impl.BaseClassificationNode;
import com.tarzan.maxkb4j.core.workflow.node.condition.impl.BaseConditionNode;
import com.tarzan.maxkb4j.core.workflow.node.database.impl.DatabaseNode;
import com.tarzan.maxkb4j.core.workflow.node.directreply.impl.BaseReplyNode;
import com.tarzan.maxkb4j.core.workflow.node.documentextract.impl.BaseDocumentExtractNode;
import com.tarzan.maxkb4j.core.workflow.node.echarts.impl.BaseEchartsNode;
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
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.impl.BaseTextToSpeechNode;
import com.tarzan.maxkb4j.core.workflow.node.userselect.impl.UserSelectNode;
import com.tarzan.maxkb4j.core.workflow.node.variableassign.impl.BaseVariableAssignNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class NodeFactory {

    private static INode getNode(String type) {
        NodeType nodeType = NodeType.getByKey(type);
        if (nodeType == null) {
            throw new IllegalStateException("不支持的节点类型: " + type);
        }
        return switch (nodeType) {
            case START -> new BaseStartNode();
            case AI_CHAT -> new BaseChatNode();
            case SEARCH_KNOWLEDGE -> new BaseSearchDatasetNode();
            case USER_SELECT -> new UserSelectNode();
            case CONDITION -> new BaseConditionNode();
            case CLASSIFICATION -> new BaseClassificationNode();
            case REPLY-> new BaseReplyNode();
            case APPLICATION -> new BaseApplicationNode();
            case QUESTION -> new BaseQuestionNode();
            case IMAGE_GENERATE -> new BaseImageGenerateNode();
            case TEXT_TO_SPEECH -> new BaseTextToSpeechNode();
            case DOCUMENT_EXTRACT -> new BaseDocumentExtractNode();
            case SPEECH_TO_TEXT -> new BaseSpeechToTextNode();
            case VARIABLE_ASSIGN -> new BaseVariableAssignNode();
            case FUNCTION -> new BaseFunctionNode();
            case IMAGE_UNDERSTAND -> new BaseImageUnderstandNode();
            case RERANKER -> new RerankerNode();
            case FORM -> new FormNode();
            case MCP -> new BaseMcpNode();
            case ECHARTS -> new BaseEchartsNode();
            case DATABASE -> new DatabaseNode();
            default -> null;
        };

    }

    public static INode getNode(String nodeType, LfNode lfNode, FlowParams workflowParams, WorkflowManage workflowManage, List<String> lastNodeIds, Function<LfNode, JSONObject> getNodeParams) {
        INode node=getNode(nodeType);
        if(Objects.nonNull(node)){
            node.setId(lfNode.getId());
            node.setType(nodeType);
            node.setLfNode(lfNode);
            node.setWorkflowParams(workflowParams);
            node.setWorkflowManage(workflowManage);
            node.setLastNodeIdList(lastNodeIds);
            if(Objects.nonNull(getNodeParams)){
                node.setNodeParams(getNodeParams.apply(lfNode));
            }
            return node;
        }
        return null;
    }

    public static INode getNode(String nodeType, LfNode node, FlowParams workflowParams, WorkflowManage workflowManage) {
        return getNode(nodeType,node,workflowParams,workflowManage,new ArrayList<>(),null);
    }



}
