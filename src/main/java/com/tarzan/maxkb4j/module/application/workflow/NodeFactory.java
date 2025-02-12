package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.impl.BaseChatNode;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.impl.BaseApplicationNode;
import com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.impl.BaseConditionNode;
import com.tarzan.maxkb4j.module.application.workflow.node.directreplynode.impl.BaseReplyNode;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.impl.BaseSearchDatasetNode;
import com.tarzan.maxkb4j.module.application.workflow.node.startnode.impl.BaseStartNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class NodeFactory {

    private static final List<INode> nodeList = new ArrayList<>();

    static {
        // 初始化节点列表
        nodeList.add(new BaseStartNode());
        nodeList.add(new BaseChatNode());
        nodeList.add(new BaseApplicationNode());
        nodeList.add(new BaseSearchDatasetNode());
        nodeList.add(new BaseConditionNode());
        nodeList.add(new BaseReplyNode());
        // 添加其他节点...
    }

    private static INode getNode(String nodeType) {
        for (INode node : nodeList) {
            if (node.getType().equals(nodeType)) {
                return node;
            }
        }
        return null;
    }

    public static INode getNode(String nodeType, Node node, FlowParams workflowParams, WorkflowManage workflowManage) {
        INode inode=getNode(nodeType);
        if(Objects.nonNull(inode)){
            inode.setId(node.getId());
            inode.setType(nodeType);
            inode.setNode(node);
            inode.setLastNodeIdList(new ArrayList<>());
            inode.setWorkflowParams(workflowParams);
            inode.setWorkflowManage(workflowManage);
            inode.setNodeChunk(new NodeChunk());
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
