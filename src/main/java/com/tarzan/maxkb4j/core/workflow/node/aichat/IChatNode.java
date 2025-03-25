package com.tarzan.maxkb4j.core.workflow.node.aichat;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.aichat.input.ChatNodeParams;

import java.util.Objects;

public abstract class IChatNode extends INode {

    @Override
    public String getType() {
        return "ai-chat-node";
    }

    @Override
    public ChatNodeParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new ChatNodeParams();
        }
        return nodeParams.toJavaObject(ChatNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(ChatNodeParams nodeParams, FlowParams workflowParams);


}
