package com.tarzan.maxkb4j.module.application.workflow.node.condition;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.node.condition.dto.ConditionNodeParams;

import java.util.Objects;

public abstract class IConditionNode extends INode {

    @Override
    public String getType() {
        return "condition-node";
    }

    @Override
    public ConditionNodeParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new ConditionNodeParams();
        }
        return nodeParams.toJavaObject(ConditionNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams));
    }



    public abstract NodeResult execute(ConditionNodeParams nodeParams);
}
