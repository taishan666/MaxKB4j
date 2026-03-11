package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;

import static com.maxkb4j.workflow.enums.NodeType.TOOL_LIB;


public class ToolLibNode extends ToolNode {
    public ToolLibNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(TOOL_LIB.getKey());
    }
}
