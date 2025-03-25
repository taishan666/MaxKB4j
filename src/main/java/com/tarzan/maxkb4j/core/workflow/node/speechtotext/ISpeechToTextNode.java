package com.tarzan.maxkb4j.core.workflow.node.speechtotext;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.speechtotext.input.SpeechToTextParams;

public abstract class ISpeechToTextNode extends INode {
    @Override
    public String getType() {
        return "speech-to-text-node";
    }

    @Override
    public SpeechToTextParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(SpeechToTextParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(SpeechToTextParams nodeParams, FlowParams workflowParams);

}
