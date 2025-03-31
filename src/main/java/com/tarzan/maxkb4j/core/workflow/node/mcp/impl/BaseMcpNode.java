package com.tarzan.maxkb4j.core.workflow.node.mcp.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.mcp.IMcpNode;
import com.tarzan.maxkb4j.core.workflow.node.mcp.input.McpParams;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;

public class BaseMcpNode extends IMcpNode {
    @Override
    public NodeResult execute(McpParams nodeParams, FlowParams workflowParams) {
        return null;
    }

    @Override
    public JSONObject getDetail() {
        return null;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
