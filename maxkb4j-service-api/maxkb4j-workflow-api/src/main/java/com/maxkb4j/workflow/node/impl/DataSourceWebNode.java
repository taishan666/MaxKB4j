package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.DATA_SOURCE_WEB;


public class DataSourceWebNode extends AbsNode {
    public DataSourceWebNode(String id, JSONObject properties) {
        super(id, properties);
        this.setType(DATA_SOURCE_WEB.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
    }
}
