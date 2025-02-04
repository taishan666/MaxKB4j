package com.tarzan.maxkb4j.module.application.workflow.node.applicationnode;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.dto.ApplicationNodeParams;

public abstract class IApplicationNode extends INode {


    @Override
    public String getType() {
        return "application-node";
    }

    @Override
    public ApplicationNodeParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(ApplicationNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(ApplicationNodeParams nodeParams, FlowParams workflowParams);
}
