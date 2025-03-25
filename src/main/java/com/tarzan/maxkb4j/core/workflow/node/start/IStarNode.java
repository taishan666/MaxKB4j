package com.tarzan.maxkb4j.core.workflow.node.start;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;

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
        return this.execute(super.workflowManage.getParams(),super.workflowManage);
    }

    public abstract NodeResult execute(FlowParams workflowParams, WorkflowManage workflowManage);
}
