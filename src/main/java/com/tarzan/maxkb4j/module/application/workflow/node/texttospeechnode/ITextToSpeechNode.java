package com.tarzan.maxkb4j.module.application.workflow.node.texttospeechnode;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.texttospeechnode.dto.TextToSpeechParams;

public abstract class ITextToSpeechNode extends INode {
    @Override
    public String getType() {
        return "text-to-speech-node";
    }

    @Override
    public TextToSpeechParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(TextToSpeechParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(TextToSpeechParams nodeParams, FlowParams workflowParams);
}
