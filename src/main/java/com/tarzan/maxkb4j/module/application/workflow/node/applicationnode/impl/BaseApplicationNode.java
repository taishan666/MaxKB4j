package com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.impl;

import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.NodeDetail;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.IApplicationNode;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.dto.ApplicationNodeParams;

public class BaseApplicationNode extends IApplicationNode {

    @Override
    public NodeResult execute(ApplicationNodeParams nodeParams, FlowParams workflowParams) {
        return null;
    }

    @Override
    public void saveContext(NodeDetail nodeDetail, WorkflowManage workflowManage) {
        this.context.put("question", nodeDetail.getQuestion());
        this.context.put("run_time", nodeDetail.getRuntime());
        this.context.put("document", nodeDetail.getDocumentList());
        this.context.put("image", nodeDetail.getImageList());
        this.context.put("audio", nodeDetail.getAudioList());
    }

}