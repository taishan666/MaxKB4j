package com.tarzan.maxkb4j.module.application.workflow.node.startnode;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;

public abstract class IStarNode extends INode {

    @Override
    public String getType() {
        return "start-node";
    }

    @Override
    public BaseParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(BaseParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(super.workflowParams);
    }

    public abstract NodeResult execute(FlowParams workflowParams);
}
