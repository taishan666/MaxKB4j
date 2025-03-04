package com.tarzan.maxkb4j.module.application.workflow.node.formcollect;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.node.formcollect.input.FormNodeParams;

public abstract class IFormNode extends INode {
    @Override
    public String getType() {
        return "form-node";
    }

    @Override
    public String getViewType() {
        return "single_view";
    }

    @Override
    public FormNodeParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(FormNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams));
    }


    public abstract NodeResult execute(FormNodeParams nodeParams);
}
