package com.tarzan.maxkb4j.core.workflow.node.variableassign;


import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.variableassign.input.VariableAssignParams;

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
