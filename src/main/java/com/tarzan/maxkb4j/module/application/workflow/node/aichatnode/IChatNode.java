package com.tarzan.maxkb4j.module.application.workflow.node.aichatnode;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.dto.ChatNodeParams;

public abstract class IChatNode extends INode {

    String type="ai-chat-node";

    @Override
    public ChatNodeParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(ChatNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(ChatNodeParams nodeParams, FlowParams workflowParams);
}
