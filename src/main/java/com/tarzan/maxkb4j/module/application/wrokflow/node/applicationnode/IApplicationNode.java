package com.tarzan.maxkb4j.module.application.wrokflow.node.applicationnode;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.wrokflow.INode;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.NodeResult;
import com.tarzan.maxkb4j.module.application.wrokflow.node.applicationnode.dto.ApplicationNodeParams;

public abstract class IApplicationNode extends INode {

    String type="application-node";

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
