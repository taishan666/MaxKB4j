package com.tarzan.maxkb4j.core.workflow.node.texttospeech;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.input.TextToSpeechParams;

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
