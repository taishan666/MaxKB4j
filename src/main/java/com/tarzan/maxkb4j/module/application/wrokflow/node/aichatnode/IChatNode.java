package com.tarzan.maxkb4j.module.application.wrokflow.node.aichatnode;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.wrokflow.INode;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.NodeResult;
import com.tarzan.maxkb4j.module.application.wrokflow.node.aichatnode.dto.ChatNodeParams;

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
