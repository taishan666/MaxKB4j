package com.tarzan.maxkb4j.module.application.wrokflow.node.applicationnode.impl;

import com.tarzan.maxkb4j.module.application.wrokflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.wrokflow.NodeResult;
import com.tarzan.maxkb4j.module.application.wrokflow.node.applicationnode.dto.ApplicationNodeParams;
import com.tarzan.maxkb4j.module.application.wrokflow.node.applicationnode.IApplicationNode;

public class BaseApplicationNode extends IApplicationNode {

    @Override
    public NodeResult execute(ApplicationNodeParams nodeParams, FlowParams workflowParams) {
        return null;
    }
}
