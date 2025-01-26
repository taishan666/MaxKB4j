package com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.impl;

import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.dto.ApplicationNodeParams;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.IApplicationNode;

public class BaseApplicationNode extends IApplicationNode {

    @Override
    public NodeResult execute(ApplicationNodeParams nodeParams, FlowParams workflowParams) {
        return null;
    }
}
