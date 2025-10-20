package com.tarzan.maxkb4j.core.workflow.node.tool.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TOOL;

public class ToolNode extends INode {
    public ToolNode(JSONObject properties) {
        super(properties);
        this.setType(TOOL.getKey());
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }


}
