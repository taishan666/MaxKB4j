package com.tarzan.maxkb4j.module.application.workflow.node.reranker;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.reranker.input.RerankerParams;

public abstract class IRerankerNode extends INode {
    @Override
    public String getType() {
        return "reranker-node";
    }

    @Override
    public RerankerParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(RerankerParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(RerankerParams nodeParams, FlowParams workflowParams);
}
