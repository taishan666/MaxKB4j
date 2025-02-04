package com.tarzan.maxkb4j.module.application.workflow;

import com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.impl.BaseChatNode;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.impl.BaseApplicationNode;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.impl.BaseSearchDatasetNode;
import com.tarzan.maxkb4j.module.application.workflow.node.startnode.impl.BaseStartStepNode;

import java.util.ArrayList;
import java.util.List;

public class NodeFactory {

    private static final List<INode> nodeList = new ArrayList<>();

    static {
        // 初始化节点列表
        nodeList.add(new BaseStartStepNode());
        nodeList.add(new BaseChatNode());
        nodeList.add(new BaseApplicationNode());
        nodeList.add(new BaseSearchDatasetNode());
        // 添加其他节点...
    }

    public static INode getNode(String nodeType) {
        System.out.println("nodeType="+nodeType);
        System.out.println("nodeList="+nodeList);
        for (INode node : nodeList) {
            if (node.getType().equals(nodeType)) {
                return node;
            }
        }
        return null;
    }
}
