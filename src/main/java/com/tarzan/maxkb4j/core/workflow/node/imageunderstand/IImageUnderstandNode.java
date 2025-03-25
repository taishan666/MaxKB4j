package com.tarzan.maxkb4j.core.workflow.node.imageunderstand;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.imageunderstand.input.ImageUnderstandParams;

public abstract class IImageUnderstandNode extends INode {
    @Override
    public String getType() {
        return "image-understand-node";
    }


    @Override
    public ImageUnderstandParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(ImageUnderstandParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(ImageUnderstandParams nodeParams, FlowParams workflowParams);
}
