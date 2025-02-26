package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.aichat.impl.BaseChatNode;
import com.tarzan.maxkb4j.module.application.workflow.node.application.impl.BaseApplicationNode;
import com.tarzan.maxkb4j.module.application.workflow.node.condition.impl.BaseConditionNode;
import com.tarzan.maxkb4j.module.application.workflow.node.directreply.impl.BaseReplyNode;
import com.tarzan.maxkb4j.module.application.workflow.node.documentextract.impl.BaseDocumentExtractNode;
import com.tarzan.maxkb4j.module.application.workflow.node.imagegenerate.impl.BaseImageGenerateNode;
import com.tarzan.maxkb4j.module.application.workflow.node.question.impl.BaseQuestionNode;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdataset.impl.BaseSearchDatasetNode;
import com.tarzan.maxkb4j.module.application.workflow.node.speechtotext.impl.BaseSpeechToTextNode;
import com.tarzan.maxkb4j.module.application.workflow.node.start.impl.BaseStartNode;
import com.tarzan.maxkb4j.module.application.workflow.node.texttospeech.impl.BaseTextToSpeechNode;
import com.tarzan.maxkb4j.module.application.workflow.node.variableassign.impl.BaseVariableAssignNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class NodeFactory {

    private final List<INode> nodeList;

    public NodeFactory() {
        // 初始化 node_list
        nodeList = new ArrayList<>();
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
    }

    private  INode getNode(String nodeType) {
        for (INode node : nodeList) {
            if (node.getType().equals(nodeType)) {
                return node;
            }
        }
        return null;
    }

    public static INode getNode(String nodeType, Node node, FlowParams workflowParams, WorkflowManage workflowManage) {
        NodeFactory nodeFactory = new NodeFactory();
        INode inode=nodeFactory.getNode(nodeType);
        if(Objects.nonNull(inode)){
            inode.setId(node.getId());
            inode.setType(nodeType);
            inode.setNode(node);
            //inode.setLastNodeIdList(new ArrayList<>());
            inode.setWorkflowParams(workflowParams);
            inode.setWorkflowManage(workflowManage);
           // inode.setNodeChunk(new NodeChunk());
            return inode;
        }
        return null;
    }

    public static INode getNode(String nodeType, Node node, FlowParams workflowParams, WorkflowManage workflowManage, List<String> lastNodeIds, Function<Node, JSONObject> getNodeParams) {
        INode inode=getNode(nodeType, node, workflowParams, workflowManage);
        if(Objects.nonNull(inode)){
            inode.setLastNodeIdList(lastNodeIds);
            if(Objects.nonNull(getNodeParams)){
                inode.setNodeParams(getNodeParams.apply(node));
            }
            return inode;
        }
        return null;
    }
}
