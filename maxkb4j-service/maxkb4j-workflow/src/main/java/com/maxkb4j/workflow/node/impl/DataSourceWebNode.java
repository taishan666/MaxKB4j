package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.node.AbsNode;

import static com.maxkb4j.workflow.enums.NodeType.DATA_SOURCE_WEB;


public class DataSourceWebNode extends AbsNode {
    public DataSourceWebNode(String id, JSONObject properties) {
        super(id, properties);
        this.setType(DATA_SOURCE_WEB.getKey());
    }

}
