package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.AbsNode;

@NodeCreatorType(NodeType.BASE)
public class BaseNode extends AbsNode {

    public BaseNode(String id, JSONObject properties) {
        super(id,properties);
    }

}
