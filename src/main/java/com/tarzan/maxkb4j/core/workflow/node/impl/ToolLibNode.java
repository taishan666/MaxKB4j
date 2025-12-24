package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TOOL_LIB;

public class ToolLibNode extends ToolNode {
    public ToolLibNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(TOOL_LIB.getKey());
    }
}
