package com.tarzan.maxkb4j.core.workflow.node.directreply;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.directreply.input.ReplyNodeParams;

import java.util.Objects;

public abstract class IReplyNode extends INode {

    public String getType() {
        return "reply-node";
    }
    @Override
    public ReplyNodeParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new ReplyNodeParams();
        }
        return nodeParams.toJavaObject(ReplyNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams));
    }

    public abstract NodeResult execute(ReplyNodeParams nodeParams);
}
