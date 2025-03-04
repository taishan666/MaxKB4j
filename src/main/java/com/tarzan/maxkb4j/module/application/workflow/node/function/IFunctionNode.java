package com.tarzan.maxkb4j.module.application.workflow.node.function;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.node.function.input.FunctionParams;

public abstract class IFunctionNode extends INode {
    @Override
    public String getType() {
        return "function-node";
    }

    @Override
    public FunctionParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(FunctionParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams));
    }



    public abstract NodeResult execute(FunctionParams nodeParams);

}
