package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.info.Node;
import com.tarzan.maxkb4j.core.workflow.node.aichat.impl.BaseChatNode;
import com.tarzan.maxkb4j.core.workflow.node.application.impl.BaseApplicationNode;
import com.tarzan.maxkb4j.core.workflow.node.condition.impl.BaseConditionNode;
import com.tarzan.maxkb4j.core.workflow.node.directreply.impl.BaseReplyNode;
import com.tarzan.maxkb4j.core.workflow.node.documentextract.impl.BaseDocumentExtractNode;
import com.tarzan.maxkb4j.core.workflow.node.formcollect.impl.FormNode;
import com.tarzan.maxkb4j.core.workflow.node.function.impl.BaseFunctionNode;
import com.tarzan.maxkb4j.core.workflow.node.imagegenerate.impl.BaseImageGenerateNode;
import com.tarzan.maxkb4j.core.workflow.node.imageunderstand.impl.BaseImageUnderstandNode;
import com.tarzan.maxkb4j.core.workflow.node.question.impl.BaseQuestionNode;
import com.tarzan.maxkb4j.core.workflow.node.reranker.impl.RerankerNode;
import com.tarzan.maxkb4j.core.workflow.node.searchdataset.impl.BaseSearchDatasetNode;
import com.tarzan.maxkb4j.core.workflow.node.speechtotext.impl.BaseSpeechToTextNode;
import com.tarzan.maxkb4j.core.workflow.node.start.impl.BaseStartNode;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.impl.BaseTextToSpeechNode;
import com.tarzan.maxkb4j.core.workflow.node.variableassign.impl.BaseVariableAssignNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class NodeFactory {

    private static final List<INode> nodeList = new ArrayList<>();;


    public static void InitNodes(){
        // 初始化 node_list
        nodeList.add(new BaseStartNode());
        nodeList.add(new BaseChatNode());
        nodeList.add(new BaseSearchDatasetNode());
        nodeList.add(new BaseConditionNode());
        nodeList.add(new BaseReplyNode());
        nodeList.add(new BaseApplicationNode());
        nodeList.add(new BaseQuestionNode());
        nodeList.add(new BaseImageGenerateNode());
        nodeList.add(new BaseTextToSpeechNode());
        nodeList.add(new BaseDocumentExtractNode());
        nodeList.add(new BaseSpeechToTextNode());
        nodeList.add(new BaseVariableAssignNode());
        nodeList.add(new BaseFunctionNode());
        nodeList.add(new BaseImageUnderstandNode());
        nodeList.add(new RerankerNode());
        nodeList.add(new FormNode());
    }

    private static INode getNode(String nodeType) {
        for (INode node : nodeList) {
            if (node.getType().equals(nodeType)) {
                node.setStatus(200);
                node.setErrMessage("");
                node.setNodeChunk(new NodeChunk());
                return node;
            }
        }
        return null;
    }

    public static INode getNode(String nodeType, Node node, FlowParams workflowParams, WorkflowManage workflowManage) {
        return getNode(nodeType,node,workflowParams,workflowManage,new ArrayList<>(),null);
    }

    public static INode getNode(String nodeType, Node node, FlowParams workflowParams, WorkflowManage workflowManage, List<String> lastNodeIds, Function<Node, JSONObject> getNodeParams) {
        INode inode=getNode(nodeType);
        if(Objects.nonNull(inode)){
            inode.setId(node.getId());
            inode.setType(nodeType);
            inode.setNode(node);
            inode.setWorkflowParams(workflowParams);
            inode.setWorkflowManage(workflowManage);
            inode.setLastNodeIdList(lastNodeIds);
            if(Objects.nonNull(getNodeParams)){
                inode.setNodeParams(getNodeParams.apply(node));
            }
            return inode;
        }
        return null;
    }

}
