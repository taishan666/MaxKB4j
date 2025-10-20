package com.tarzan.maxkb4j.core.workflow.node.toollib.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TOOL_LIB;

public class ToolLibNode extends INode {
    public ToolLibNode(JSONObject properties) {
        super(properties);
        this.setType(TOOL_LIB.getKey());
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }


}
