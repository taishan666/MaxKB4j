package com.tarzan.maxkb4j.core.workflow.node.mcp.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.MCP;

public class McpNode extends INode {

    public McpNode(JSONObject properties) {
        super(properties);
        this.setType(MCP.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("result", detail.get("result"));
    }


}
