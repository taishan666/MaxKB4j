package com.tarzan.maxkb4j.core.workflow.node.application;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.application.input.ApplicationNodeParams;

import java.util.Objects;

public abstract class IApplicationNode extends INode {


    @Override
    public String getType() {
        return "application-node";
    }

    @Override
    public ApplicationNodeParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new ApplicationNodeParams();
        }
        return nodeParams.toJavaObject(ApplicationNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(ApplicationNodeParams nodeParams, FlowParams workflowParams);
}
