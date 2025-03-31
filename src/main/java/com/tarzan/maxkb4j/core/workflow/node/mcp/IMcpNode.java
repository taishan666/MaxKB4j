package com.tarzan.maxkb4j.core.workflow.node.mcp;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.mcp.input.McpParams;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;

import java.util.Objects;

public abstract class IMcpNode extends INode {
    @Override
    public String getType() {
        return "mcp-node";
    }
    @Override
    public McpParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new McpParams();
        }
        return nodeParams.toJavaObject(McpParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(McpParams nodeParams, FlowParams workflowParams);
}
