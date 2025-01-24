package com.tarzan.maxkb4j.module.application.wrokflow.node.startnode;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.wrokflow.INode;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.BaseParams;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.NodeResult;

public abstract class IStarNode extends INode {
    String type="start-node";

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
