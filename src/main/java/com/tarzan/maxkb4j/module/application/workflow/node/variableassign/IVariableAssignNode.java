package com.tarzan.maxkb4j.module.application.workflow.node.variableassign;


import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.variableassign.input.VariableAssignParams;

public abstract class IVariableAssignNode extends INode {
    @Override
    public String getType() {
        return "variable-assign-node";
    }

    @Override
    public VariableAssignParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(VariableAssignParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams), super.workflowParams);
    }

    public abstract NodeResult execute(VariableAssignParams nodeParams, FlowParams workflowParams);
}
